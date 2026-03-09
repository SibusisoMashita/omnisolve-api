package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DocumentTypeResponse(
        @Schema(description = "Document type ID", example = "1")
        Long id,
        
        @Schema(description = "Document type name", example = "Policy")
        String name,
        
        @Schema(description = "Document type description", example = "High-level organizational policies")
        String description,
        
        @Schema(description = "Indicates whether documents of this type must be linked to ISO clauses", example = "true")
        Boolean requiresClauses
) {
}
