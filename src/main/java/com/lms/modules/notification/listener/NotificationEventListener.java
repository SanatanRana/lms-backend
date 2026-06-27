package com.lms.modules.notification.listener;

import com.lms.common.event.DomainEvents;
import com.lms.modules.notification.service.NotificationService;
import com.lms.modules.course.repository.EnrollmentRepository;
import com.lms.modules.course.entity.EnrollmentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SYSTEM DESIGN: Open-Closed Principle (OCP) in Action
 * ─────────────────────────────────────────────────────
 * This listener AUTOMATICALLY fires when domain events are published.
 * It was added without modifying AuthService, CourseService, or PaymentService.
 *
 * To add a new reaction to any event (e.g., send SMS, log analytics):
 *   1. Create a new @Component class with an @EventListener method.
 *   2. Done. Zero changes to existing services or controllers.
 *
 * @Async ensures these run on background threads – the HTTP response
 * is sent BEFORE these complete, keeping response times fast.
 */
@Component
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Async("taskExecutor")
    @EventListener
    public void onUserRegistered(DomainEvents.UserRegisteredEvent event) {
        notificationService.createNotification(
                event.userId(),
                "Welcome to the Platform! 🎉",
                "Hi " + event.name() + ", your account has been created successfully. " +
                "Start exploring courses now!"
        );
    }

    @Async("taskExecutor")
    @EventListener
    public void onCourseEnrolled(DomainEvents.CourseEnrolledEvent event) {
        notificationService.createNotification(
                event.studentId(),
                "Enrollment Confirmed! 📚",
                "You have been successfully enrolled in the course. Start learning now!"
        );
    }

    @Async("taskExecutor")
    @EventListener
    public void onPaymentCompleted(DomainEvents.PaymentCompletedEvent event) {
        notificationService.createNotification(
                event.userId(),
                "Payment Successful! ✅",
                "Your payment of ₹" + event.amount() + " has been processed successfully."
        );
    }

    @Async("taskExecutor")
    @EventListener
    public void onPaymentFailed(DomainEvents.PaymentFailedEvent event) {
        notificationService.createNotification(
                event.userId(),
                "Payment Failed ❌",
                "Your payment could not be processed. Reason: " + event.reason() + 
                ". Please try again."
        );
    }

    @Async("taskExecutor")
    @EventListener
    public void onLiveSessionScheduled(DomainEvents.LiveSessionScheduledEvent event) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByCourseId(event.courseId());
        for (EnrollmentEntity enrollment : enrollments) {
            notificationService.createNotification(
                enrollment.getStudent().getId(),
                "📅 New Live Class Scheduled",
                "A new live class \"" + event.title() + "\" has been scheduled."
            );
        }
    }

    @Async("taskExecutor")
    @EventListener
    public void onLiveSessionRescheduled(DomainEvents.LiveSessionRescheduledEvent event) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByCourseId(event.courseId());
        for (EnrollmentEntity enrollment : enrollments) {
            notificationService.createNotification(
                enrollment.getStudent().getId(),
                "🔄 Live Class Rescheduled",
                "The live class \"" + event.title() + "\" has been rescheduled."
            );
        }
    }

    @Async("taskExecutor")
    @EventListener
    public void onLiveSessionStarted(DomainEvents.LiveSessionStartedEvent event) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByCourseId(event.courseId());
        for (EnrollmentEntity enrollment : enrollments) {
            notificationService.createNotification(
                enrollment.getStudent().getId(),
                "🔴 Live Class Started!",
                "The live class \"" + event.title() + "\" has started. Join the classroom now!"
            );
        }
    }

    @Async("taskExecutor")
    @EventListener
    public void onLiveSessionCancelled(DomainEvents.LiveSessionCancelledEvent event) {
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByCourseId(event.courseId());
        for (EnrollmentEntity enrollment : enrollments) {
            notificationService.createNotification(
                enrollment.getStudent().getId(),
                "❌ Live Class Cancelled",
                "The live class \"" + event.title() + "\" has been cancelled."
            );
        }
    }
}
