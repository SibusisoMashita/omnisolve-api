package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record IncidentInvestigationRequest(
        @Schema(description = "Investigator user ID", example = "user-456", required = true)
        @NotBlank(message = "Investigator ID is required")
        String investigatorId,

        @Schema(description = "Analysis method used", example = "5 Whys")
        String analysisMethod,

        @Schema(description = "Root cause identified", example = "Equipment maintenance was overdue")
        String rootCause,

        @Schema(description = "Investigation findings", example = "Detailed findings from the investigation")
        String findings
) {
}
