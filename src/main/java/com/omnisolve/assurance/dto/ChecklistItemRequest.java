package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChecklistItemRequest(
        @Schema(description = "Item title", example = "Check tyre pressure", required = true)
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Detailed description or instructions")
        String description,

        @Schema(description = "Display order position", required = true)
        @NotNull(message = "Sort order is required")
        Integer sortOrder
) {
}
