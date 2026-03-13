package com.omnisolve.contractor.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractorWorkerResponse(
        UUID id,
        UUID contractorId,
        String firstName,
        String lastName,
        String idNumber,
        String phone,
        String email,
        String status,
        OffsetDateTime createdAt
) {}
