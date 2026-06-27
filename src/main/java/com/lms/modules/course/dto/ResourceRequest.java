package com.lms.modules.course.dto;

import lombok.Data;

@Data
public class ResourceRequest {
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long fileSize;
}
