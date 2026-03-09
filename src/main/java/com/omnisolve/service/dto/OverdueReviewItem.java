package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OverdueReviewItem(
        UUID id,
        String documentNumber,
        String title,
        String departmentName,
        OffsetDateTime nextReviewAt,
        long daysOverdue
) {
}

