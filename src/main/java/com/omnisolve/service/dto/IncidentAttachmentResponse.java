package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentAttachmentResponse(
        Long id,
        UUID incidentId,
        String fileName,
        Long fileSize,
        String mimeType,
        String s3Key,
        String uploadedBy,
        OffsetDateTime uploadedAt
) {
}
