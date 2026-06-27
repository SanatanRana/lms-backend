package com.lms.modules.live.service;

import com.lms.common.enums.SessionStatus;
import com.lms.common.event.DomainEvents;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.entity.EnrollmentEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.course.repository.EnrollmentRepository;
import com.lms.modules.live.dto.LiveSessionRequest;
import com.lms.modules.live.entity.AttendanceEntity;
import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.modules.live.repository.AttendanceRepository;
import com.lms.modules.live.repository.LiveSessionRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LiveSessionService {

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public LiveSessionEntity createSession(LiveSessionRequest request, String teacherEmail) {
        UserEntity teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        CourseEntity course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        LiveSessionEntity session = new LiveSessionEntity();
        session.setCourse(course);
        session.setTeacher(teacher);
        session.setTitle(request.getTitle());
        session.setMeetingLink(request.getMeetingLink());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setStatus(SessionStatus.SCHEDULED);

        LiveSessionEntity saved = liveSessionRepository.save(session);
        eventPublisher.publishEvent(new DomainEvents.LiveSessionScheduledEvent(
                saved.getId(), saved.getCourse().getId(), saved.getTitle(), saved.getStartTime()
        ));
        return saved;
    }

    @Transactional
    public LiveSessionEntity startSession(Long sessionId) {
        LiveSessionEntity session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus(SessionStatus.LIVE);
        session.setStartTime(LocalDateTime.now());
        LiveSessionEntity saved = liveSessionRepository.save(session);

        eventPublisher.publishEvent(new DomainEvents.LiveSessionStartedEvent(
                saved.getId(), saved.getCourse().getId(), saved.getTitle()
        ));
        return saved;
    }

    @Transactional
    public LiveSessionEntity endSession(Long sessionId) {
        LiveSessionEntity session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus(SessionStatus.ENDED);
        session.setEndTime(LocalDateTime.now());
        LiveSessionEntity saved = liveSessionRepository.save(session);

        eventPublisher.publishEvent(new DomainEvents.LiveSessionEndedEvent(
                saved.getId(), saved.getCourse().getId()
        ));
        return saved;
    }

    @Transactional
    public AttendanceEntity joinSession(Long sessionId, String studentEmail) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        LiveSessionEntity session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() == SessionStatus.ENDED) {
            throw new RuntimeException("Session has already ended");
        }

        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setStudent(student);
        attendance.setLiveSession(session);
        attendance.setJoinTime(LocalDateTime.now());
        return attendanceRepository.save(attendance);
    }

    @Transactional
    public AttendanceEntity leaveSession(Long sessionId, String studentEmail) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        AttendanceEntity attendance = attendanceRepository
                .findByStudentIdAndLiveSessionIdAndLeaveTimeIsNull(student.getId(), sessionId)
                .orElseThrow(() -> new RuntimeException("Active attendance record not found"));

        attendance.setLeaveTime(LocalDateTime.now());
        return attendanceRepository.save(attendance);
    }

    @Transactional(readOnly = true)
    public List<LiveSessionEntity> getSessionsByCourse(Long courseId) {
        return liveSessionRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<LiveSessionEntity> getSessionsByTeacher(String teacherEmail) {
        UserEntity teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        return liveSessionRepository.findByTeacherId(teacher.getId());
    }

    @Transactional(readOnly = true)
    public List<AttendanceEntity> getSessionAttendance(Long sessionId) {
        return attendanceRepository.findByLiveSessionId(sessionId);
    }

    @Transactional
    public LiveSessionEntity updateSession(Long sessionId, LiveSessionRequest request, String teacherEmail) {
        LiveSessionEntity session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("You are not authorized to update this session");
        }

        if (session.getStatus() == SessionStatus.ENDED) {
            throw new RuntimeException("Cannot update an ended session");
        }

        session.setTitle(request.getTitle());
        session.setMeetingLink(request.getMeetingLink());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        LiveSessionEntity saved = liveSessionRepository.save(session);

        eventPublisher.publishEvent(new DomainEvents.LiveSessionRescheduledEvent(
                saved.getId(), saved.getCourse().getId(), saved.getTitle(), saved.getStartTime()
        ));
        return saved;
    }

    @Transactional
    public void deleteSession(Long sessionId, String teacherEmail) {
        LiveSessionEntity session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("You are not authorized to delete this session");
        }

        // Delete attendance records first to avoid foreign key constraints
        List<AttendanceEntity> attendances = attendanceRepository.findByLiveSessionId(sessionId);
        attendanceRepository.deleteAll(attendances);

        liveSessionRepository.delete(session);

        eventPublisher.publishEvent(new DomainEvents.LiveSessionCancelledEvent(
                session.getCourse().getId(), session.getTitle()
        ));
    }

    @Transactional(readOnly = true)
    public List<LiveSessionEntity> getSessionsForEnrolledCourses(String studentEmail) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentId(student.getId());
        List<Long> courseIds = enrollments.stream()
                .map(e -> e.getCourse().getId())
                .collect(java.util.stream.Collectors.toList());

        if (courseIds.isEmpty()) {
            return List.of();
        }

        return liveSessionRepository.findByCourseIdIn(courseIds).stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED || s.getStatus() == SessionStatus.LIVE)
                .sorted((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()))
                .collect(java.util.stream.Collectors.toList());
    }
}
