package com.lms.modules.course.dto;

import lombok.Data;

@Data
public class LessonRequest {
    private String title;
    private String description;
    private String videoUrl;
}
