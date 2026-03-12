package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record IncidentActionUpdateRequest(
        @Schema(description = "Action status", example = "Completed")
        String status,

        @Schema(description = "Completion timestamp", example = "2026-03-15T14:30:00Z")
        OffsetDateTime completedAt
) {
}
