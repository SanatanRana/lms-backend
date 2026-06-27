package com.lms.modules.course.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.course.service.AzureBlobStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
            }
            String fileUrl = azureBlobStorageService.uploadFile(file);
            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileType", file.getContentType());
            data.put("fileSize", String.valueOf(file.getSize()));

            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully to Azure Blob Storage", data));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }
}
