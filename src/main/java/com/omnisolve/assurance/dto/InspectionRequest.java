package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InspectionRequest(
        @Schema(description = "Asset UUID to inspect", required = true)
        @NotNull(message = "Asset ID is required")
        UUID assetId,

        @Schema(description = "Inspection title", example = "Daily Vehicle Pre-Trip", required = true)
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Inspection type ID")
        Long inspectionTypeId,

        @Schema(description = "Inspector user ID")
        String inspectorId,

        @Schema(description = "Scheduled date/time")
        OffsetDateTime scheduledAt,

        @Schema(description = "Checklist template ID to pre-populate items")
        Long checklistId
) {
}
