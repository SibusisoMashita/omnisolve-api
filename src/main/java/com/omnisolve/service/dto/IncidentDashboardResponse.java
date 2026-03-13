package com.omnisolve.service.dto;

public record IncidentDashboardResponse(
        long totalIncidents,
        long openIncidents,
        long highSeverityIncidents,
        double averageClosureTimeDays
) {
}
