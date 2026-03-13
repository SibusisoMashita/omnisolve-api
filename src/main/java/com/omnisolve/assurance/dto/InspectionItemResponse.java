package com.omnisolve.assurance.dto;

import java.util.UUID;

public record InspectionItemResponse(
        Long id,
        UUID inspectionId,
        Long checklistItemId,
        String checklistItemTitle,
        String status,
        String notes
) {
}
