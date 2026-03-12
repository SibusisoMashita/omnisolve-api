package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record IncidentActionRequest(
        @Schema(description = "Action title", example = "Schedule equipment maintenance", required = true)
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Action description", example = "Perform full maintenance check on machine")
        String description,

        @Schema(description = "User ID assigned to this action", example = "user-789")
        String assignedTo,

        @Schema(description = "Due date for completion", example = "2026-03-20")
        LocalDate dueDate
) {
}
