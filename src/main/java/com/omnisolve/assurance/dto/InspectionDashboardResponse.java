package com.omnisolve.assurance.dto;

public record InspectionDashboardResponse(
        long totalInspections,
        long scheduled,
        long inProgress,
        long completed,
        long overdue
) {
}
