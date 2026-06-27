package com.lms.modules.live.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LiveSessionRequest {
    private Long courseId;
    private String title;
    private String meetingLink;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
