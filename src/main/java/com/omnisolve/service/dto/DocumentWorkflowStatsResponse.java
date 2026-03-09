package com.omnisolve.service.dto;

public record DocumentWorkflowStatsResponse(
        long draft,
        long pending,
        long active,
        long archived
) {
}

