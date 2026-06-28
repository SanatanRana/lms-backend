package com.lms.modules.live.entity;

import com.lms.common.enums.RecordingStatus;
import com.lms.common.enums.SessionStatus;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.user.entity.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_sessions", indexes = {
    @Index(name = "idx_live_course", columnList = "course_id"),
    @Index(name = "idx_live_teacher", columnList = "teacher_id"),
    @Index(name = "idx_live_status", columnList = "status"),
    @Index(name = "idx_live_room_token", columnList = "room_token", unique = true)
})
@Data
public class LiveSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "sections"})
    private CourseEntity course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private UserEntity teacher;

    @Column(nullable = false)
    private String title;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(name = "room_token", unique = true)
    private String roomToken;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "recording_url", length = 1024)
    private String recordingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "recording_status", nullable = false)
    private RecordingStatus recordingStatus = RecordingStatus.NONE;

    @Column(name = "max_participants")
    private Integer maxParticipants = 50;

    @Column(name = "chat_enabled")
    private Boolean chatEnabled = true;

    @Column(name = "guest_access_enabled")
    private Boolean guestAccessEnabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
