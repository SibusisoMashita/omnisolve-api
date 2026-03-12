package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentCommentResponse(
        Long id,
        UUID incidentId,
        String comment,
        String createdBy,
        OffsetDateTime createdAt
) {
}
