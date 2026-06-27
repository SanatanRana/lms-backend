package com.lms.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureStorageConfig {

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @Bean
    public BlobServiceClient blobServiceClient() {
        if (connectionString == null || connectionString.trim().isEmpty() || connectionString.contains("YOUR_ACCOUNT_KEY")) {
            System.err.println("WARNING: Azure Storage Connection String is not configured. File upload features will be disabled.");
            return null;
        }
        try {
            return new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        } catch (Exception e) {
            System.err.println("WARNING: Failed to initialize Azure BlobServiceClient: " + e.getMessage());
            return null;
        }
    }
}
