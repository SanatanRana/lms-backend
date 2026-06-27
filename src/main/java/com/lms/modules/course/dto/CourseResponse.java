package com.lms.modules.course.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Lightweight response DTO – prevents leaking password hashes,
 * lazy-loading proxies, and deep entity graphs to the frontend.
 */
@Data
public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private Double discountPrice;
    private String thumbnailUrl;
    private String introVideoUrl;
    private String courseType;
    private String category;
    private String teacherName;
    private Long teacherId;
    private LocalDateTime createdAt;
    private int sectionCount;
}
