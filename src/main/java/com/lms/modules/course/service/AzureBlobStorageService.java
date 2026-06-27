package com.lms.modules.course.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AzureBlobStorageService {

    @Autowired(required = false)
    private BlobServiceClient blobServiceClient;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public String uploadFile(MultipartFile file) throws IOException {
        if (blobServiceClient == null) {
            throw new IllegalStateException("Azure Blob Storage is not configured. Please set the 'azure.storage.connection-string' in your application.properties file.");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueName = UUID.randomUUID().toString() + extension;

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }
        try {
            // Set container access policy to allow public read access for blobs
            containerClient.setAccessPolicy(PublicAccessType.BLOB, null);
        } catch (Exception e) {
            System.err.println("Warning: Could not set container access policy to public read: " + e.getMessage());
        }

        BlobClient blobClient = containerClient.getBlobClient(uniqueName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // Set Content-Type header so the browser knows how to render/play the file
        if (file.getContentType() != null) {
            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(file.getContentType());
            blobClient.setHttpHeaders(headers);
        }

        return blobClient.getBlobUrl();
    }
}
