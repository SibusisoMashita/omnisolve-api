package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record StandardResponse(
        @Schema(description = "Standard ID", example = "1")
        Long id,

        @Schema(description = "Standard code", example = "ISO-9001")
        String code,

        @Schema(description = "Standard name", example = "ISO 9001:2015")
        String name,

        @Schema(description = "Standard version", example = "2015")
        String version,

        @Schema(description = "Creation timestamp")
        OffsetDateTime createdAt
) {
}