package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PendingApprovalItem(
        UUID id,
        String documentNumber,
        String title,
        String departmentName,
        String ownerId,
        OffsetDateTime submittedAt
) {
}
