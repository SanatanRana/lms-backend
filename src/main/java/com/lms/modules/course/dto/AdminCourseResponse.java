package com.lms.modules.course.dto;

import lombok.Data;

@Data
public class AdminCourseResponse {
    private Long id;
    private String title;
    private String category;
    private Double price;
    private Double discountPrice;
    private String teacherName;
    private String teacherEmail;
    private long enrolledStudentsCount;
}
