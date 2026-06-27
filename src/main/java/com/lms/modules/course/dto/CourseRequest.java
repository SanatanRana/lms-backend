package com.lms.modules.course.dto;

import lombok.Data;

@Data
public class CourseRequest {
    private String title;
    private String description;
    private Double price;
    private Double discountPrice;
    private String thumbnailUrl;
    private String introVideoUrl;
    private String category;
}