package com.omnisolve.contractor.service.dto;

public record ContractorWorkerRequest(
        String firstName,
        String lastName,
        String idNumber,
        String phone,
        String email,
        String status
) {}
