package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record IncidentUpdateRequest(
        @Schema(description = "Title of the incident", example = "Equipment malfunction in production area")
        String title,

        @Schema(description = "Detailed description of the incident")
        String description,

        @Schema(description = "Severity ID", example = "2")
        Long severityId,

        @Schema(description = "Department ID", example = "3")
        Long departmentId,

        @Schema(description = "Investigator user ID", example = "user-456")
        String investigatorId
) {
}
