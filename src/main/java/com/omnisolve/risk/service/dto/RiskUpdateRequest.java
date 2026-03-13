package com.omnisolve.risk.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record RiskUpdateRequest(

        @Schema(description = "Updated title")
        String title,

        @Schema(description = "Updated description")
        String description,

        @Schema(description = "Updated category ID")
        Long categoryId,

        @Schema(description = "Updated severity ID")
        Long severityId,

        @Schema(description = "Updated likelihood ID")
        Long likelihoodId,

        @Schema(description = "Updated owner user ID")
        String ownerId,

        @Schema(description = "Updated status", allowableValues = {"OPEN", "MITIGATED", "ACCEPTED", "CLOSED"})
        String status,

        @Schema(description = "Updated review date")
        LocalDate reviewDate
) {
}
