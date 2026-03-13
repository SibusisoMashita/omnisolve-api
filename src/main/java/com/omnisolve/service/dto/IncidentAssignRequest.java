package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record IncidentAssignRequest(
        @Schema(description = "Investigator user ID", example = "user-456", required = true)
        @NotBlank(message = "Investigator ID is required")
        String investigatorId
) {
}
