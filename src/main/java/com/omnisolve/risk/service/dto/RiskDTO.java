package com.omnisolve.risk.service.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RiskDTO(
        UUID id,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        Long severityId,
        String severityName,
        Long likelihoodId,
        String likelihoodName,
        Integer riskScore,
        String status,
        String ownerId,
        LocalDate reviewDate,
        OffsetDateTime identifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RiskControlDTO> controls,
        List<RiskAttachmentDTO> attachments
) {
}
