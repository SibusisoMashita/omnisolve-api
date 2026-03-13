package com.omnisolve.risk.controller;

import com.omnisolve.risk.service.RiskMetadataService;
import com.omnisolve.risk.service.dto.RiskOptionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Risk Metadata", description = "Reference data for risk categories, severities, and likelihoods")
public class RiskMetadataController {

    private final RiskMetadataService metadataService;

    public RiskMetadataController(RiskMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/api/risk/categories")
    @Operation(summary = "List all risk categories")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Categories retrieved successfully"))
    public List<RiskOptionDTO> listCategories() {
        return metadataService.getMetadata().categories();
    }

    @GetMapping("/api/risk/likelihoods")
    @Operation(summary = "List all risk likelihoods ordered by level")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Likelihoods retrieved successfully"))
    public List<RiskOptionDTO> listLikelihoods() {
        return metadataService.getMetadata().likelihoods();
    }

    @GetMapping("/api/risk/severities")
    @Operation(summary = "List all risk severities ordered by level")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Severities retrieved successfully"))
    public List<RiskOptionDTO> listSeverities() {
        return metadataService.getMetadata().severities();
    }
}
