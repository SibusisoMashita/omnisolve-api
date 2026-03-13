package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record IncidentStatusChangeRequest(
        @Schema(description = "Status ID", example = "3", required = true)
        @NotNull(message = "Status ID is required")
        Long statusId
) {
}
