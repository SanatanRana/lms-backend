package com.lms.common.event;

/**
 * Domain events – published by services, consumed by @EventListener beans.
 * 
 * SYSTEM DESIGN: Open-Closed Principle (OCP)
 * ───────────────────────────────────────────
 * Adding a new reaction to any domain action (e.g., send an SMS on enrollment,
 * award a badge on payment, log an audit on course creation) requires ONLY
 * creating a new @Component with @EventListener – zero changes to existing code.
 *
 * Records are immutable value objects – ideal for thread-safe async processing.
 */
public final class DomainEvents {

    private DomainEvents() { /* namespace only */ }

    // ── Auth Events ────────────────────────────────────────────────
    public record UserRegisteredEvent(Long userId, String email, String name, String role) {}

    // ── Course Events ──────────────────────────────────────────────
    public record CourseCreatedEvent(Long courseId, String title, Long teacherId) {}
    public record CourseDeletedEvent(Long courseId) {}

    // ── Enrollment Events ──────────────────────────────────────────
    public record CourseEnrolledEvent(Long enrollmentId, Long studentId, Long courseId) {}

    // ── Payment Events ─────────────────────────────────────────────
    public record PaymentCompletedEvent(Long paymentId, Long userId, Long courseId, Double amount) {}
    public record PaymentFailedEvent(Long paymentId, Long userId, Long courseId, String reason) {}

    // ── Live Session Events ────────────────────────────────────────
    public record LiveSessionStartedEvent(Long sessionId, Long courseId, String title) {}
    public record LiveSessionEndedEvent(Long sessionId, Long courseId) {}
    public record LiveSessionScheduledEvent(Long sessionId, Long courseId, String title, java.time.LocalDateTime startTime) {}
    public record LiveSessionRescheduledEvent(Long sessionId, Long courseId, String title, java.time.LocalDateTime startTime) {}
    public record LiveSessionCancelledEvent(Long courseId, String title) {}

    // ── Assignment Events ──────────────────────────────────────────
    public record AssignmentSubmittedEvent(Long submissionId, Long studentId, Long assignmentId) {}
}
