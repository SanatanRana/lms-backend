package com.lms.modules.user.service;

import com.lms.common.enums.Role;
import com.lms.common.enums.EnrollmentStatus;
import com.lms.common.event.DomainEvents;
import com.lms.modules.course.dto.AdminCourseResponse;
import com.lms.modules.course.dto.ManualEnrollmentRequest;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.entity.EnrollmentEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.course.repository.EnrollmentRepository;
import com.lms.modules.payment.dto.PaymentResponse;
import com.lms.modules.payment.entity.CouponEntity;
import com.lms.modules.payment.entity.PaymentEntity;
import com.lms.modules.payment.repository.CouponRepository;
import com.lms.modules.payment.repository.PaymentRepository;
import com.lms.modules.payment.service.PaymentService;
import com.lms.modules.user.dto.AdminUserDetailResponse;
import com.lms.modules.user.dto.AnalyticsResponse;
import com.lms.modules.user.dto.MonthlyRevenueStats;
import com.lms.modules.user.dto.UserResponse;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.lms.modules.auth.dto.RegisterRequest;
import com.lms.modules.notification.repository.NotificationRepository;
import com.lms.modules.notification.entity.NotificationEntity;
import com.lms.modules.ai.repository.AiChatMessageRepository;
import com.lms.modules.ai.entity.AiChatMessageEntity;
import com.lms.modules.live.repository.AttendanceRepository;
import com.lms.modules.live.entity.AttendanceEntity;
import com.lms.modules.live.repository.LiveSessionRepository;
import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.modules.assignment.repository.AssignmentRepository;
import com.lms.modules.assignment.entity.AssignmentEntity;
import com.lms.modules.assignment.repository.AssignmentSubmissionRepository;
import com.lms.modules.assignment.entity.AssignmentSubmissionEntity;
import com.lms.modules.course.repository.CourseResourceRepository;
import com.lms.modules.course.entity.CourseResourceEntity;

@Service
public class AdminService {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AiChatMessageRepository aiChatMessageRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private LiveSessionRepository liveSessionRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Autowired private CourseResourceRepository courseResourceRepository;
    @Autowired private PaymentService paymentService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse toggleUserActiveStatus(Long id, String adminEmail) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new RuntimeException("You cannot suspend or activate your own account!");
        }
        user.setActive(!user.isActive());
        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getPlatformAnalytics() {
        AnalyticsResponse response = new AnalyticsResponse();
        response.setTotalStudents(userRepository.countByRole(Role.STUDENT));
        response.setTotalTeachers(userRepository.countByRole(Role.TEACHER));
        response.setTotalCourses(courseRepository.count());
        response.setTotalEnrollments(enrollmentRepository.count());
        response.setTotalRevenue(paymentRepository.sumSuccessfulPayments());

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime monthStart = LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0);
        response.setMonthlyRevenue(paymentRepository.sumSuccessfulPaymentsSince(monthStart));

        LocalDateTime yearStart = LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
        response.setYearlyRevenue(paymentRepository.sumSuccessfulPaymentsSince(yearStart));

        List<MonthlyRevenueStats> monthlyStats = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime temp = now.minusMonths(i);
            LocalDateTime tempStart = LocalDateTime.of(temp.getYear(), temp.getMonth(), 1, 0, 0);
            LocalDateTime tempEnd = tempStart.plusMonths(1).minusNanos(1);
            Double monthlySum = paymentRepository.sumSuccessfulPaymentsBetween(tempStart, tempEnd);
            String monthName = temp.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            monthlyStats.add(new MonthlyRevenueStats(monthName + " " + temp.getYear(), monthlySum));
        }
        response.setMonthlyStats(monthlyStats);

        return response;
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetails(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AdminUserDetailResponse details = new AdminUserDetailResponse();
        details.setId(user.getId());
        details.setName(user.getName());
        details.setEmail(user.getEmail());
        details.setPhone(user.getPhone());
        details.setRole(user.getRole());
        details.setActive(user.isActive());
        details.setCreatedAt(user.getCreatedAt());

        if (user.getRole() == Role.STUDENT) {
            List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdWithCourse(user.getId());
            List<AdminUserDetailResponse.EnrolledCourseDetail> enrolledDetails = enrollments.stream().map(e -> {
                AdminUserDetailResponse.EnrolledCourseDetail item = new AdminUserDetailResponse.EnrolledCourseDetail();
                item.setCourseId(e.getCourse().getId());
                item.setTitle(e.getCourse().getTitle());
                item.setCategory(e.getCourse().getCategory());
                item.setProgressPercent(e.getProgressPercent());
                item.setEnrolledAt(e.getEnrolledAt());
                return item;
            }).collect(Collectors.toList());
            details.setEnrolledCourses(enrolledDetails);
        } else if (user.getRole() == Role.TEACHER) {
            List<CourseEntity> courses = courseRepository.findByTeacherId(user.getId());
            List<AdminUserDetailResponse.CreatedCourseDetail> createdDetails = courses.stream().map(c -> {
                AdminUserDetailResponse.CreatedCourseDetail item = new AdminUserDetailResponse.CreatedCourseDetail();
                item.setCourseId(c.getId());
                item.setTitle(c.getTitle());
                item.setCategory(c.getCategory());
                item.setPrice(c.getPrice());
                item.setEnrolledStudentsCount(enrollmentRepository.countByCourseId(c.getId()));
                return item;
            }).collect(Collectors.toList());
            details.setCreatedCourses(createdDetails);
        }

        return details;
    }

    @Transactional(readOnly = true)
    public List<AdminCourseResponse> getAllCourses() {
        List<CourseEntity> courses = courseRepository.findAll();
        if (courses.isEmpty()) return java.util.Collections.emptyList();

        List<Long> courseIds = courses.stream().map(CourseEntity::getId).collect(Collectors.toList());
        List<Object[]> enrollmentsData = enrollmentRepository.countEnrollmentsByCourseIds(courseIds);
        Map<Long, Long> enrollMap = enrollmentsData.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1], (a, b) -> a));

        return courses.stream().map(c -> {
            AdminCourseResponse resp = new AdminCourseResponse();
            resp.setId(c.getId());
            resp.setTitle(c.getTitle());
            resp.setCategory(c.getCategory());
            resp.setPrice(c.getPrice());
            resp.setDiscountPrice(c.getDiscountPrice());
            if (c.getTeacher() != null) {
                resp.setTeacherName(c.getTeacher().getName());
                resp.setTeacherEmail(c.getTeacher().getEmail());
            }
            resp.setEnrolledStudentsCount(enrollMap.getOrDefault(c.getId(), 0L));
            return resp;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentEntity> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllTransactions() {
        return paymentService.getAllPayments();
    }

    @Transactional(readOnly = true)
    public List<CouponEntity> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Transactional
    public EnrollmentEntity enrollStudentManually(ManualEnrollmentRequest request) {
        UserEntity student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("The selected user is not a Student");
        }

        CourseEntity course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new RuntimeException("Student is already enrolled in this course");
        }

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setProgressPercent(0);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        EnrollmentEntity saved = enrollmentRepository.save(enrollment);

        eventPublisher.publishEvent(new DomainEvents.CourseEnrolledEvent(
                saved.getId(), student.getId(), course.getId()
        ));

        return saved;
    }

    @Transactional
    public void revokeEnrollment(Long enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new RuntimeException("Enrollment not found");
        }
        enrollmentRepository.deleteById(enrollmentId);
    }

    @Transactional
    public CouponEntity createCoupon(com.lms.modules.payment.dto.CouponRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (code.isEmpty()) throw new RuntimeException("Coupon code cannot be empty");
        if (couponRepository.findByCode(code).isPresent()) throw new RuntimeException("Coupon code already exists");
        if (request.getDiscountPercent() == null || request.getDiscountPercent() < 1 || request.getDiscountPercent() > 100) {
            throw new RuntimeException("Discount percent must be between 1 and 100");
        }

        CouponEntity coupon = new CouponEntity();
        coupon.setCode(code);
        coupon.setDiscountPercent(request.getDiscountPercent());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setActive(request.isActive());
        coupon.setCurrentUses(0);

        return couponRepository.save(coupon);
    }

    @Transactional
    public CouponEntity updateCoupon(Long id, com.lms.modules.payment.dto.CouponRequest request) {
        CouponEntity coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        String code = request.getCode().trim().toUpperCase();
        if (code.isEmpty()) throw new RuntimeException("Coupon code cannot be empty");
        if (!coupon.getCode().equals(code) && couponRepository.findByCode(code).isPresent()) {
            throw new RuntimeException("Coupon code already exists");
        }
        if (request.getDiscountPercent() == null || request.getDiscountPercent() < 1 || request.getDiscountPercent() > 100) {
            throw new RuntimeException("Discount percent must be between 1 and 100");
        }

        coupon.setCode(code);
        coupon.setDiscountPercent(request.getDiscountPercent());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setActive(request.isActive());

        return couponRepository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) throw new RuntimeException("Coupon not found");
        couponRepository.deleteById(id);
    }

    @Transactional
    public UserResponse registerTeacher(RegisterRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) throw new RuntimeException("Name cannot be empty!");
        if (request.getEmail() == null || !request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) throw new RuntimeException("Invalid email format!");
        if (request.getPassword() == null || request.getPassword().length() < 6) throw new RuntimeException("Password must be at least 6 characters long!");
        if (userRepository.findByEmail(request.getEmail()).isPresent()) throw new RuntimeException("Email is already registered!");

        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone() != null ? request.getPhone() : "");
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.TEACHER);
        user.setActive(true);

        UserEntity saved = userRepository.save(user);

        eventPublisher.publishEvent(new DomainEvents.UserRegisteredEvent(
                saved.getId(), saved.getEmail(), saved.getName(), saved.getRole().name()
        ));

        return toUserResponse(saved);
    }

    /**
     * Safe user deletion using ordered JPA cascades.
     * Replaces the dangerous SET FOREIGN_KEY_CHECKS = 0 approach.
     */
    @Transactional
    public void deleteUser(Long id, String adminEmail) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new RuntimeException("You cannot delete your own account!");
        }

        // 1. Delete notifications
        notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedAtDesc(id));

        // 2. Delete AI chat messages
        aiChatMessageRepository.deleteAll(aiChatMessageRepository.findByUserIdOrderByCreatedAtAsc(id));

        // 3. Delete attendance records
        attendanceRepository.deleteAll(attendanceRepository.findByStudentId(id));

        // 4. Delete assignment submissions
        assignmentSubmissionRepository.deleteAll(assignmentSubmissionRepository.findByStudentId(id));

        // 5. Delete enrollments
        enrollmentRepository.deleteAll(enrollmentRepository.findByStudentId(id));

        // 6. Delete payments
        paymentRepository.deleteAll(paymentRepository.findByUserId(id));

        // 7. Delete courses and dependent objects if user is a teacher
        if (user.getRole() == Role.TEACHER) {
            List<CourseEntity> teacherCourses = courseRepository.findByTeacherId(id);
            for (CourseEntity course : teacherCourses) {
                // Delete enrollments of this course
                enrollmentRepository.deleteAll(enrollmentRepository.findByCourseId(course.getId()));
                // Delete payments of this course
                paymentRepository.deleteAll(paymentRepository.findByCourseId(course.getId()));
                // Delete live sessions and their attendance
                for (LiveSessionEntity live : liveSessionRepository.findByCourseId(course.getId())) {
                    attendanceRepository.deleteAll(attendanceRepository.findByLiveSessionId(live.getId()));
                    liveSessionRepository.delete(live);
                }
                // Delete assignments and their submissions
                for (AssignmentEntity assign : assignmentRepository.findByCourseId(course.getId())) {
                    assignmentSubmissionRepository.deleteAll(assignmentSubmissionRepository.findByAssignmentId(assign.getId()));
                    assignmentRepository.delete(assign);
                }
                // Delete course resources
                courseResourceRepository.deleteAll(courseResourceRepository.findByCourseId(course.getId()));
                // Delete course itself
                courseRepository.delete(course);
            }

            // Delete live sessions created by teacher directly (outside courses)
            for (LiveSessionEntity live : liveSessionRepository.findByTeacherId(id)) {
                attendanceRepository.deleteAll(attendanceRepository.findByLiveSessionId(live.getId()));
                liveSessionRepository.delete(live);
            }
        }

        // 8. Finally delete the user
        userRepository.delete(user);
    }

    // ── Helper ──────────────────────────────────────────────────────
    private UserResponse toUserResponse(UserEntity user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setName(user.getName());
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setRole(user.getRole());
        r.setActive(user.isActive());
        r.setCreatedAt(user.getCreatedAt());
        return r;
    }
}
