package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InspectionFindingRequest(
        @Schema(description = "Severity ID (FK to inspection_severities)", required = true)
        @NotNull(message = "Severity ID is required")
        Long severityId,

        @Schema(description = "Description of the finding", required = true)
        @NotBlank(message = "Description is required")
        String description,

        @Schema(description = "Whether corrective action is required")
        boolean actionRequired,

        @Schema(description = "ISO clause ID to link this finding for compliance reporting")
        Long clauseId
) {
}
