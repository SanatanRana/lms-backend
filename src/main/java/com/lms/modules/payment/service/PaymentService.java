package com.lms.modules.payment.service;

import com.lms.common.enums.PaymentStatus;
import com.lms.common.event.DomainEvents;
import com.lms.common.strategy.PaymentGateway;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.course.service.EnrollmentService;
import com.lms.modules.payment.dto.CreateOrderRequest;
import com.lms.modules.payment.dto.VerifyPaymentRequest;
import com.lms.modules.payment.entity.CouponEntity;
import com.lms.modules.payment.entity.PaymentEntity;
import com.lms.modules.payment.repository.CouponRepository;
import com.lms.modules.payment.repository.PaymentRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private List<PaymentGateway> paymentGateways; // Spring injects ALL implementations

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * SYSTEM DESIGN: Strategy Pattern
     * This method finds the correct gateway dynamically.
     * Adding a new gateway = adding a new @Component class. Zero code changes here.
     */
    private PaymentGateway getGateway(String gatewayId) {
        return paymentGateways.stream()
                .filter(g -> g.getGatewayId().equals(gatewayId))
                .findFirst()
                .orElse(paymentGateways.get(0)); // fallback to first available
    }

    @Transactional
    public Map<String, Object> createOrder(CreateOrderRequest request, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CourseEntity course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

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
            coupon.setCurrentUses(coupon.getCurrentUses() + 1);
            couponRepository.save(coupon);
        }

        // Use payment gateway (strategy pattern)
        PaymentGateway gateway = getGateway("mock"); // configurable
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
        response.put("gateway", gateway.getGatewayId());
        response.put("paymentId", payment.getId());
        return response;
    }

    @Transactional
    public PaymentEntity verifyPayment(VerifyPaymentRequest request, String userEmail) {
        PaymentEntity payment = paymentRepository.findByGatewayOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment order not found"));

        PaymentGateway gateway = getGateway(payment.getPaymentMethod());

        Map<String, String> gatewayData = request.getGatewayData() != null ? request.getGatewayData() : new HashMap<>();
        gatewayData.put("orderId", request.getOrderId());
        gatewayData.put("transactionId", request.getTransactionId());

        if (gateway.verifyPayment(gatewayData)) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(request.getTransactionId());
            paymentRepository.save(payment);

            // Auto-enroll student after successful payment
            enrollmentService.enrollStudent(payment.getCourse().getId(), userEmail);

            // OCP: Publish payment event
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
            throw new RuntimeException("Payment verification failed");
        }

        return payment;
    }

    @Transactional(readOnly = true)
    public List<PaymentEntity> getUserPayments(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return paymentRepository.findByUserId(user.getId());
    }
}
