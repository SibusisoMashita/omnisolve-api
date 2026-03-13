package com.omnisolve.assurance.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InspectionAttachmentResponse(
        Long id,
        UUID inspectionId,
        String s3Key,
        String fileName,
        Long fileSize,
        String mimeType,
        String uploadedBy,
        OffsetDateTime uploadedAt
) {
}
