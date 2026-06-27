package com.lms.modules.course.service;

import com.lms.common.enums.CourseType;
import com.lms.common.enums.EnrollmentStatus;
import com.lms.common.event.DomainEvents;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.entity.EnrollmentEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.course.repository.EnrollmentRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public EnrollmentEntity enrollStudent(Long courseId, String studentEmail) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Check if already enrolled
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new RuntimeException("You are already enrolled in this course");
        }

        // For PAID courses, check payment (simplified – in production use payment verification)
        if (course.getCourseType() == CourseType.PAID) {
            // For now, allow enrollment – payment will be verified via PaymentService
        }

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setProgressPercent(0);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        EnrollmentEntity saved = enrollmentRepository.save(enrollment);

        // OCP: Publish event – new listeners can react (welcome email, analytics, etc.)
        eventPublisher.publishEvent(new DomainEvents.CourseEnrolledEvent(
                saved.getId(), student.getId(), courseId
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentEntity> getStudentEnrollments(String studentEmail) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return enrollmentRepository.findByStudentIdWithCourse(student.getId());
    }

    @Transactional
    public EnrollmentEntity updateProgress(Long courseId, String studentEmail, int progressPercent) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        EnrollmentEntity enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollment.setProgressPercent(Math.min(progressPercent, 100));
        if (progressPercent >= 100) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
        }

        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long courseId, String studentEmail) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRole() == com.lms.common.enums.Role.ADMIN) {
            return true;
        }

        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (course.getTeacher() != null && course.getTeacher().getId().equals(student.getId())) {
            return true;
        }

        return enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
    }

    @Transactional(readOnly = true)
    public long getEnrollmentCount(Long courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }
}
