package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentInvestigationResponse(
        Long id,
        UUID incidentId,
        String investigatorId,
        String analysisMethod,
        String rootCause,
        String findings,
        OffsetDateTime createdAt
) {
}
