package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AssetTypeRequest(
        @Schema(description = "Asset type name", example = "Vehicle", required = true)
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Description of the asset type")
        String description
) {
}
