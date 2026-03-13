package com.omnisolve.risk.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RiskControlRequest(

        @Schema(description = "Description of the control measure", example = "Implement vendor security assessment process", required = true)
        @NotBlank(message = "Description is required")
        String description,

        @Schema(description = "Owner responsible for this control", example = "user-sub-id")
        String controlOwner
) {
}
