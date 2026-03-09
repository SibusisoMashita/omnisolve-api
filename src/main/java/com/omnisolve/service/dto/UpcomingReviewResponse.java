package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpcomingReviewResponse(
        UUID id,
        String documentNumber,
        String title,
        String departmentName,
        OffsetDateTime nextReviewAt,
        long daysUntilReview
) {
}

