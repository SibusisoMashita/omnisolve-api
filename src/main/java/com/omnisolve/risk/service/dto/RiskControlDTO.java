package com.omnisolve.risk.service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RiskControlDTO(
        Long id,
        UUID riskId,
        String description,
        String controlOwner,
        OffsetDateTime createdAt
) {
}
