package com.omnisolve.contractor.service.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractorDocumentResponse(
        Long id,
        UUID contractorId,
        Long documentTypeId,
        String documentTypeName,
        String s3Key,
        String fileName,
        Long fileSize,
        String mimeType,
        LocalDate issuedAt,
        LocalDate expiryDate,
        String documentStatus,
        String uploadedBy,
        OffsetDateTime uploadedAt
) {}
