package com.omnisolve.service.dto;

import java.time.OffsetDateTime;

public record DocumentRequest(
        String documentNumber,
        String title,
        String summary,
        Long typeId,
        Long departmentId,
        String ownerId,
        OffsetDateTime nextReviewAt
) {
}

