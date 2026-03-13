package com.omnisolve.risk.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RiskAttachmentDTO(
        Long id,
        UUID riskId,
        String fileName,
        Long fileSize,
        String mimeType,
        String s3Key,
        String uploadedBy,
        OffsetDateTime uploadedAt
) {
}
