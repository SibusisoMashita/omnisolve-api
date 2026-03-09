package com.omnisolve.service.dto;

import java.time.OffsetDateTime;

public record DocumentVersionResponse(
        Long id,
        Integer versionNumber,
        String fileName,
        String mimeType,
        Long fileSize,
        String s3Key,
        OffsetDateTime uploadedAt,
        String uploadedBy
) {
}

