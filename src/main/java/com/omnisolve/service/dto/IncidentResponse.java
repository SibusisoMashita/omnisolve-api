package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String incidentNumber,
        String title,
        String description,
        Long typeId,
        String typeName,
        Long severityId,
        String severityName,
        Long statusId,
        String statusName,
        Long departmentId,
        String departmentName,
        Long siteId,
        String siteName,
        String investigatorId,
        OffsetDateTime occurredAt,
        String reportedBy,
        OffsetDateTime closedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
