package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record IncidentRequest(
        @Schema(description = "Title of the incident", example = "Equipment malfunction in production area", required = true)
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Detailed description of the incident", example = "Machine stopped unexpectedly during operation")
        String description,

        @Schema(description = "Incident type ID", example = "1", required = true)
        @NotNull(message = "Type ID is required")
        Long typeId,

        @Schema(description = "Severity ID", example = "2", required = true)
        @NotNull(message = "Severity ID is required")
        Long severityId,

        @Schema(description = "Department ID", example = "3")
        Long departmentId,

        @Schema(description = "Site ID", example = "1")
        Long siteId,

        @Schema(description = "When the incident occurred", example = "2026-03-12T10:30:00Z", required = true)
        @NotNull(message = "Occurred at is required")
        OffsetDateTime occurredAt
) {
}
