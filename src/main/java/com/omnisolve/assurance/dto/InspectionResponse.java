package com.omnisolve.assurance.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InspectionResponse(
        UUID id,
        Long organisationId,
        UUID assetId,
        String assetName,
        Long inspectionTypeId,
        String inspectionTypeName,
        String inspectionNumber,
        String title,
        String inspectorId,
        String status,
        OffsetDateTime scheduledAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
