package com.lms.modules.payment.service;

import com.lms.common.enums.PaymentStatus;
import com.lms.common.event.DomainEvents;
import com.lms.common.strategy.PaymentGateway;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.course.service.EnrollmentService;
import com.lms.modules.payment.dto.CreateOrderRequest;
import com.lms.modules.payment.dto.PaymentResponse;
import com.lms.modules.payment.dto.VerifyPaymentRequest;
import com.lms.modules.payment.entity.CouponEntity;
import com.lms.modules.payment.entity.PaymentEntity;
import com.lms.modules.payment.repository.CouponRepository;
import com.lms.modules.payment.repository.PaymentRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private List<PaymentGateway> paymentGateways;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${payment.gateway:mock}")
    private String defaultGateway;

    /**
     * SYSTEM DESIGN: Strategy Pattern
     * Finds the correct gateway by ID. Throws clearly if gateway not found —
     * never silently falls back to Mock in production.
     */
    private PaymentGateway getGateway(String gatewayId) {
        return paymentGateways.stream()
                .filter(g -> g.getGatewayId().equals(gatewayId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "Payment gateway '" + gatewayId + "' is not configured. " +
                    "Available gateways: " + paymentGateways.stream()
                        .map(PaymentGateway::getGatewayId)
                        .collect(Collectors.joining(", "))
                ));
    }

    @Transactional
    public Map<String, Object> createOrder(CreateOrderRequest request, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CourseEntity course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.isActive()) {
            throw new RuntimeException("Cannot purchase an archived or unpublished course");
        }

        // Prevent duplicate enrollment
        if (enrollmentService.isEnrolled(course.getId(), userEmail)) {
            throw new RuntimeException("You are already enrolled in this course");
        }

        Double finalAmount = course.getPrice();

        // Apply coupon if provided
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponEntity coupon = couponRepository.findByCodeAndActiveTrue(request.getCouponCode())
                    .orElseThrow(() -> new RuntimeException("Invalid or expired coupon"));

            if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("Coupon has expired");
            }
            if (coupon.getMaxUses() != null && coupon.getCurrentUses() >= coupon.getMaxUses()) {
                throw new RuntimeException("Coupon usage limit reached");
            }

            finalAmount = finalAmount - (finalAmount * coupon.getDiscountPercent() / 100.0);
            // Round to 2 decimal places for currency
            finalAmount = Math.round(finalAmount * 100.0) / 100.0;
            coupon.setCurrentUses(coupon.getCurrentUses() + 1);
            couponRepository.save(coupon);
        }

        // Use payment gateway (strategy pattern — reads from application.properties)
        PaymentGateway gateway = getGateway(defaultGateway);
        Map<String, String> order = gateway.createOrder(finalAmount, "INR", course.getTitle());

        // Persist payment record
        PaymentEntity payment = new PaymentEntity();
        payment.setUser(user);
        payment.setCourse(course);
        payment.setAmount(finalAmount);
        payment.setPaymentMethod(gateway.getGatewayId());
        payment.setGatewayOrderId(order.get("orderId"));
        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get("orderId"));
        response.put("amount", finalAmount);
        response.put("currency", "INR");
        response.put("gateway", gateway.getGatewayId());
        response.put("paymentId", payment.getId());
        response.put("courseName", course.getTitle());
        // Include Razorpay-specific fields if present
        if (order.containsKey("razorpayOrderId")) {
            response.put("razorpayOrderId", order.get("razorpayOrderId"));
        }
        return response;
    }

    @Transactional
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, String userEmail) {
        PaymentEntity payment = paymentRepository.findByGatewayOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment order not found"));

        // Verify the payment belongs to this user
        if (!payment.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Payment verification failed: unauthorized");
        }

        // Prevent double-verification
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toPaymentResponse(payment);
        }

        PaymentGateway gateway = getGateway(payment.getPaymentMethod());

        Map<String, String> gatewayData = request.getGatewayData() != null
                ? new HashMap<>(request.getGatewayData())
                : new HashMap<>();
        gatewayData.put("orderId", request.getOrderId());
        gatewayData.put("transactionId", request.getTransactionId());

        if (gateway.verifyPayment(gatewayData)) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(request.getTransactionId());
            paymentRepository.save(payment);

            // Auto-enroll student after successful payment
            enrollmentService.enrollStudent(payment.getCourse().getId(), userEmail);

            eventPublisher.publishEvent(new DomainEvents.PaymentCompletedEvent(
                    payment.getId(), payment.getUser().getId(),
                    payment.getCourse().getId(), payment.getAmount()
            ));
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            eventPublisher.publishEvent(new DomainEvents.PaymentFailedEvent(
                    payment.getId(), payment.getUser().getId(),
                    payment.getCourse().getId(), "Gateway verification failed"
            ));
            throw new RuntimeException("Payment verification failed. Please contact support.");
        }

        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return paymentRepository.findByUserId(user.getId()).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @org.springframework.context.event.EventListener
    @Transactional
    public void onCourseDeleted(DomainEvents.CourseDeletedEvent event) {
        List<PaymentEntity> payments = paymentRepository.findByCourseId(event.courseId());
        paymentRepository.deleteAll(payments);
    }

    // ── DTO Mapping ─────────────────────────────────────────────────
    public PaymentResponse toPaymentResponse(PaymentEntity payment) {
        PaymentResponse r = new PaymentResponse();
        r.setId(payment.getId());
        r.setUserId(payment.getUser() != null ? payment.getUser().getId() : null);
        r.setUserName(payment.getUser() != null ? payment.getUser().getName() : null);
        r.setCourseId(payment.getCourse() != null ? payment.getCourse().getId() : null);
        r.setCourseTitle(payment.getCourse() != null ? payment.getCourse().getTitle() : null);
        r.setAmount(payment.getAmount());
        r.setPaymentMethod(payment.getPaymentMethod());
        r.setTransactionId(payment.getTransactionId());
        r.setGatewayOrderId(payment.getGatewayOrderId());
        r.setPaymentStatus(payment.getPaymentStatus());
        r.setCreatedAt(payment.getCreatedAt());
        return r;
    }
}
