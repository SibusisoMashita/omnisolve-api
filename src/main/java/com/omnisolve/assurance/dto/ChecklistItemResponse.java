package com.omnisolve.assurance.dto;

public record ChecklistItemResponse(
        Long id,
        String title,
        String description,
        Integer sortOrder
) {
}
