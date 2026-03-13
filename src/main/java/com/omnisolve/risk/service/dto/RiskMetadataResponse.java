package com.omnisolve.risk.service.dto;

import java.util.List;

public record RiskMetadataResponse(
        List<RiskOptionDTO> categories,
        List<RiskOptionDTO> severities,
        List<RiskOptionDTO> likelihoods
) {
}
