package com.omnisolve.risk.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RiskControlUpdateRequest(

        @Schema(description = "Updated description of the control measure")
        String description,

        @Schema(description = "Updated control owner")
        String controlOwner
) {
}
