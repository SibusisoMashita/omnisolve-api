package com.omnisolve.service.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentActionResponse(
        Long id,
        UUID incidentId,
        String title,
        String description,
        String assignedTo,
        LocalDate dueDate,
        String status,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt
) {
}
