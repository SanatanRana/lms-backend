package com.lms.modules.assignment.dto;

import lombok.Data;

@Data
public class SubmissionRequest {
    private Long assignmentId;
    private String submissionUrl;
    private String answerText;
}
