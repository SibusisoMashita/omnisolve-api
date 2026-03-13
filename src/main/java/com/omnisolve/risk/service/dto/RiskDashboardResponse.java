package com.omnisolve.risk.service.dto;

public record RiskDashboardResponse(
        long totalRisks,
        long highRisks,
        long mediumRisks,
        long lowRisks,
        long overdueReviews
) {
}
