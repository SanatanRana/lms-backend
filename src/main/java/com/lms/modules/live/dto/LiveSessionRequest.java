package com.lms.modules.live.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LiveSessionRequest {
    private Long courseId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxParticipants;
    private Boolean chatEnabled;
    private Boolean guestAccessEnabled;
}
