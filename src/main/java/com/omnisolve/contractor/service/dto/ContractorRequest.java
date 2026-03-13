package com.omnisolve.contractor.service.dto;

import jakarta.validation.constraints.NotBlank;

public record ContractorRequest(
        @NotBlank String name,
        String registrationNumber,
        String contactPerson,
        String email,
        String phone,
        String status
) {}
