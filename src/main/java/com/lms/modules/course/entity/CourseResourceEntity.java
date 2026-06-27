package com.lms.modules.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_resources", indexes = {
    @Index(name = "idx_resource_course", columnList = "course_id")
})
@Data
public class CourseResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "sections"})
    private CourseEntity course;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 20)
    private String fileType; // PDF, DOC, ZIP, VIDEO

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize; // bytes

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
