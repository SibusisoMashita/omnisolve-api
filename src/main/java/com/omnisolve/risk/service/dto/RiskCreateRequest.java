package com.omnisolve.risk.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RiskCreateRequest(

        @Schema(description = "Title of the risk", example = "Data breach via third-party vendor", required = true)
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Detailed description of the risk")
        String description,

        @Schema(description = "Risk category ID", example = "1", required = true)
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Schema(description = "Severity ID", example = "3", required = true)
        @NotNull(message = "Severity ID is required")
        Long severityId,

        @Schema(description = "Likelihood ID", example = "2", required = true)
        @NotNull(message = "Likelihood ID is required")
        Long likelihoodId,

        @Schema(description = "Owner user ID (Cognito sub)", example = "abc-123")
        String ownerId,

        @Schema(description = "Next review date", example = "2026-09-01")
        LocalDate reviewDate
) {
}
