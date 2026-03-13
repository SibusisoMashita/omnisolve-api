package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChecklistRequest(
        @Schema(description = "Checklist name", example = "Vehicle Pre-Trip", required = true)
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Description of the checklist")
        String description,

        @Schema(description = "Asset type ID to scope this checklist (null = all asset types)")
        Long assetTypeId
) {
}
