package com.omnisolve.assurance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InspectionItemRequest(
        @Schema(description = "Checklist item ID (optional for ad-hoc items)")
        Long checklistItemId,

        @Schema(description = "Result status: PASS, FAIL, or NOT_APPLICABLE", required = true)
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "PASS|FAIL|NOT_APPLICABLE", message = "Status must be PASS, FAIL, or NOT_APPLICABLE")
        String status,

        @Schema(description = "Optional notes or observations")
        String notes
) {
}
