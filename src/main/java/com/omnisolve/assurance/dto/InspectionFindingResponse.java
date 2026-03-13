package com.omnisolve.assurance.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InspectionFindingResponse(
        Long id,
        UUID inspectionId,
        Long clauseId,
        String clauseCode,
        Long severityId,
        String severityName,
        String description,
        boolean actionRequired,
        OffsetDateTime createdAt
) {
}
