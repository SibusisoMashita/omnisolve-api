package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record ClauseResponse(
        @Schema(description = "Clause ID", example = "1")
        Long id,
        
        @Schema(description = "Standard ID", example = "1")
        Long standardId,
        
        @Schema(description = "Standard code", example = "ISO-9001")
        String standardCode,
        
        @Schema(description = "Standard name", example = "ISO 9001:2015")
        String standardName,
        
        @Schema(description = "Clause code", example = "4.1")
        String code,
        
        @Schema(description = "Clause title", example = "Understanding the organisation and its context")
        String title,
        
        @Schema(description = "Clause description")
        String description,
        
        @Schema(description = "Parent clause code for hierarchical structure", example = "4")
        String parentCode,
        
        @Schema(description = "Hierarchy level (1 = top-level)", example = "2")
        Integer level,
        
        @Schema(description = "Sort order within the same level", example = "1")
        Integer sortOrder,
        
        @Schema(description = "Creation timestamp")
        OffsetDateTime createdAt
) {
}
