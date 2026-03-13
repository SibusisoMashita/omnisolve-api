package com.omnisolve.assurance.dto;

import java.util.List;

public record ChecklistTemplateResponse(
        Long id,
        String name,
        String description,
        Long assetTypeId,
        String assetTypeName,
        List<ChecklistItemResponse> items
) {
}
