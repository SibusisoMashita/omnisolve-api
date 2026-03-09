package com.omnisolve.service.dto;

public record DocumentStatsResponse(
        long total,
        long active,
        long pending,
        long reviewDue,
        long draft,
        long archived
) {
}

