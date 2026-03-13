package com.omnisolve.assurance.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InspectionDetailResponse(
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
        OffsetDateTime updatedAt,
        List<InspectionItemResponse> items,
        List<InspectionFindingResponse> findings,
        List<InspectionAttachmentResponse> attachments
) {
}
