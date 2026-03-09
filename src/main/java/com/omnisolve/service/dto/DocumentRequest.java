package com.omnisolve.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

public record DocumentRequest(
        @Schema(description = "Title of the document", example = "Quality Management Policy", required = true)
        String title,
        
        @Schema(description = "Summary or description of the document", example = "Defines quality commitments")
        String summary,
        
        @Schema(description = "Document type ID", example = "1", required = true)
        Long typeId,
        
        @Schema(description = "Department ID", example = "1", required = true)
        Long departmentId,
        
        @Schema(description = "Owner user ID", example = "user-123", required = true)
        String ownerId,
        
        @Schema(description = "Creator full name", example = "John Doe")
        String createdBy,
        
        @Schema(description = "Next review date", example = "2027-03-09T00:00:00Z")
        OffsetDateTime nextReviewAt,
        
        @Schema(description = "List of clause IDs to link to this document", example = "[1, 2, 3]")
        List<Long> clauseIds
) {
}

