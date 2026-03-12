package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record IncidentCommentRequest(
        @Schema(description = "Comment text", example = "Investigation is in progress", required = true)
        @NotBlank(message = "Comment is required")
        String comment
) {
}
