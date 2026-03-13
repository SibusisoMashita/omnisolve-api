package com.omnisolve.assurance.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        Long organisationId,
        Long assetTypeId,
        String assetTypeName,
        String name,
        String assetTag,
        String serialNumber,
        Long siteId,
        String siteName,
        Long departmentId,
        String departmentName,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
