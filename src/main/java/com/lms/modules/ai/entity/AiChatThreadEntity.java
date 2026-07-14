package com.lms.modules.ai.entity;

import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.course.entity.CourseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_threads", indexes = {
    @Index(name = "idx_ai_thread_user_course", columnList = "user_id, course_id, created_at")
})
@Data
public class AiChatThreadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "role", "active", "phone", "createdAt", "hibernateLazyInitializer", "handler"})
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"sections", "lessons", "resources", "enrollments", "teacher", "hibernateLazyInitializer", "handler"})
    private CourseEntity course;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
