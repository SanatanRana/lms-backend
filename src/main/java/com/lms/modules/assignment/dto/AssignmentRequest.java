package com.lms.modules.assignment.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssignmentRequest {
    private Long courseId;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Integer maxScore;
}
