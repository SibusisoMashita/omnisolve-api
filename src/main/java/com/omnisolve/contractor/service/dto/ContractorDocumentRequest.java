package com.omnisolve.contractor.service.dto;

import java.time.LocalDate;

public record ContractorDocumentRequest(
        Long documentTypeId,
        LocalDate issuedAt,
        LocalDate expiryDate
) {}
