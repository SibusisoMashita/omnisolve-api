package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TagRequest(
        @Schema(description = "Tag name", example = "safety", required = true)
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Tag category for grouping")
        String category
) {
}
