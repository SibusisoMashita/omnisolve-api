package com.omnisolve.contractor.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractorResponse(
        UUID id,
        String name,
        String registrationNumber,
        String contactPerson,
        String email,
        String phone,
        String status,
        long workerCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
