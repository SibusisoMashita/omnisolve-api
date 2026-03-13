package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssetRequest(
        @Schema(description = "Asset name", example = "Truck 01", required = true)
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Asset type ID", example = "1", required = true)
        @NotNull(message = "Asset type ID is required")
        Long assetTypeId,

        @Schema(description = "Asset tag / fleet number", example = "VH-001")
        String assetTag,

        @Schema(description = "Serial number", example = "SN-123456")
        String serialNumber,

        @Schema(description = "Site ID")
        Long siteId,

        @Schema(description = "Department ID")
        Long departmentId,

        @Schema(description = "Asset status", example = "Active")
        String status
) {
}
