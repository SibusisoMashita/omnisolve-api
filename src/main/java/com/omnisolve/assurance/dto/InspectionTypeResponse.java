package com.omnisolve.assurance.dto;

public record InspectionTypeResponse(
        Long id,
        String code,
        String name,
        String description
) {
}
