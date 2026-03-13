package com.omnisolve.contractor.service.dto;

import java.util.UUID;

public record ContractorComplianceResponse(
        UUID contractorId,
        String contractorName,
        long workers,
        long requiredDocuments,
        long validDocuments,
        long expiringDocuments,
        long expiredDocuments,
        long missingDocuments,
        int complianceScore,
        String complianceStatus
) {}
