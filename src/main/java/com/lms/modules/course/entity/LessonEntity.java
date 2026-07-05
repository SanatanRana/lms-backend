package com.lms.modules.course.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "course_lessons", indexes = {
    @Index(name = "idx_lesson_section", columnList = "section_id")
})
public class LessonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "lesson_order", nullable = false)
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SectionEntity section;

    // Constructors
    public LessonEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public SectionEntity getSection() { return section; }
    public void setSection(SectionEntity section) { this.section = section; }
}
