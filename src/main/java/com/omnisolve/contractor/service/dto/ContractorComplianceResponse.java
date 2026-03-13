package com.omnisolve.contractor.service.dto;

import java.util.UUID;

public record ContractorComplianceResponse(
        UUID contractorId,
        String name,
        long requiredDocuments,
        long validDocuments,
        long expiringDocuments,
        long expiredDocuments,
        long missingDocuments,
        int complianceScore,
        String complianceStatus
) {}
