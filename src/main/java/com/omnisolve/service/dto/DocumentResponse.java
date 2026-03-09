package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String documentNumber,
        String title,
        String summary,
        String status,
        String type,
        String department,
        String ownerId,
        OffsetDateTime nextReviewAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

