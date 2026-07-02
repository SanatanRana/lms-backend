package com.lms;

import com.lms.common.enums.Role;
import com.lms.common.enums.EnrollmentStatus;
import com.lms.common.enums.PaymentStatus;
import com.lms.common.enums.SessionStatus;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import com.lms.modules.user.service.AdminService;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.course.entity.EnrollmentEntity;
import com.lms.modules.course.repository.EnrollmentRepository;
import com.lms.modules.payment.entity.PaymentEntity;
import com.lms.modules.payment.repository.PaymentRepository;
import com.lms.modules.notification.entity.NotificationEntity;
import com.lms.modules.notification.repository.NotificationRepository;
import com.lms.modules.ai.entity.AiChatMessageEntity;
import com.lms.modules.ai.repository.AiChatMessageRepository;
import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.modules.live.repository.LiveSessionRepository;
import com.lms.modules.live.entity.AttendanceEntity;
import com.lms.modules.live.repository.AttendanceRepository;
import com.lms.modules.assignment.entity.AssignmentEntity;
import com.lms.modules.assignment.repository.AssignmentRepository;
import com.lms.modules.assignment.entity.AssignmentSubmissionEntity;
import com.lms.modules.assignment.repository.AssignmentSubmissionRepository;
import com.lms.modules.course.entity.CourseResourceEntity;
import com.lms.modules.course.repository.CourseResourceRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LmsApplicationTests {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AiChatMessageRepository aiChatMessageRepository;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private CourseResourceRepository courseResourceRepository;

    @Test
    void testActivateAdmin() {
        System.out.println("=== ACTIVATING ADMIN IN DATABASE ===");
        UserEntity admin = userRepository.findByEmail("admin@lms.com").orElse(null);
        if (admin != null) {
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("Admin activated successfully!");
        } else {
            System.out.println("Admin not found!");
        }
        System.out.println("=====================================");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void testPrintDatabaseInfo() {
        System.out.println("=== LISTING ALL PAYMENTS IN DATABASE ===");
        paymentRepository.findAll().forEach(payment -> {
            System.out.printf("ID: %d, User: %s, Course: %s, Amount: %f, Status: %s, Date: %s%n",
                    payment.getId(), payment.getUser().getEmail(), payment.getCourse().getTitle(),
                    payment.getAmount(), payment.getPaymentStatus(), payment.getCreatedAt());
        });
        System.out.println("=====================================");

        System.out.println("=== LISTING ALL ENROLLMENTS IN DATABASE ===");
        enrollmentRepository.findAll().forEach(enrollment -> {
            System.out.printf("ID: %d, Student: %s, Course: %s, Status: %s%n",
                    enrollment.getId(), enrollment.getStudent().getEmail(), enrollment.getCourse().getTitle(),
                    enrollment.getStatus());
        });
        System.out.println("=====================================");

        System.out.println("=== LISTING ALL COURSES IN DATABASE ===");
        courseRepository.findAll().forEach(course -> {
            System.out.printf("ID: %d, Title: %s, Price: %f, Teacher: %s%n",
                    course.getId(), course.getTitle(), course.getPrice(),
                    course.getTeacher() != null ? course.getTeacher().getEmail() : "None");
        });
        System.out.println("=====================================");
    }
}
