package com.omnisolve.service.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String documentNumber,
        String title,
        String summary,
        Long typeId,
        String typeName,
        Boolean typeRequiresClauses,
        Long departmentId,
        String departmentName,
        Long statusId,
        String statusName,
        String ownerId,
        String createdBy,
        OffsetDateTime nextReviewAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<Long> clauseIds
) {
}

