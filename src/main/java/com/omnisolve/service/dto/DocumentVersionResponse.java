package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentVersionResponse(
        @Schema(description = "Version ID", example = "1")
        Long id,
        
        @Schema(description = "Document ID", example = "2b15d006-97f3-4695-9db1-f9fb3e16f8e9")
        UUID documentId,
        
        @Schema(description = "Version number", example = "3")
        Integer versionNumber,
        
        @Schema(description = "Original filename", example = "quality-policy-v3.pdf")
        String fileName,
        
        @Schema(description = "File size in bytes", example = "2048576")
        Long fileSize,
        
        @Schema(description = "MIME type", example = "application/pdf")
        String mimeType,
        
        @Schema(description = "S3 object key", example = "documents/2b15d006-97f3-4695-9db1-f9fb3e16f8e9/v3/quality-policy-v3.pdf")
        String s3Key,
        
        @Schema(description = "User who uploaded", example = "john.doe@company.com")
        String uploadedBy,
        
        @Schema(description = "Upload timestamp", example = "2024-01-20T14:30:00Z")
        OffsetDateTime uploadedAt
) {
}

