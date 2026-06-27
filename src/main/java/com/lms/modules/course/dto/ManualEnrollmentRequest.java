package com.lms.modules.course.dto;

import lombok.Data;

@Data
public class ManualEnrollmentRequest {
    private Long studentId;
    private Long courseId;
}
