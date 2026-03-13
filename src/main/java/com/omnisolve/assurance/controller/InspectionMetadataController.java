package com.omnisolve.assurance.controller;

import com.omnisolve.assurance.dto.InspectionSeverityResponse;
import com.omnisolve.assurance.dto.InspectionTypeResponse;
import com.omnisolve.assurance.dto.TagRequest;
import com.omnisolve.assurance.dto.TagResponse;
import com.omnisolve.assurance.service.InspectionMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Inspection Metadata", description = "Reference data for inspection types, severities and tags")
public class InspectionMetadataController {

    private final InspectionMetadataService metadataService;

    public InspectionMetadataController(InspectionMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/api/inspection-types")
    @Operation(summary = "List all inspection types")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspection types retrieved")
    })
    public List<InspectionTypeResponse> listInspectionTypes() {
        return metadataService.listInspectionTypes();
    }

    @GetMapping("/api/inspection-severities")
    @Operation(summary = "List all inspection severities ordered by level")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Severities retrieved")
    })
    public List<InspectionSeverityResponse> listInspectionSeverities() {
        return metadataService.listInspectionSeverities();
    }

    @GetMapping("/api/tags")
    @Operation(summary = "List all tags")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tags retrieved")
    })
    public List<TagResponse> listTags() {
        return metadataService.listTags();
    }

    @PostMapping("/api/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new tag")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tag created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public TagResponse createTag(@Valid @RequestBody TagRequest request) {
        return metadataService.createTag(request);
    }

    @DeleteMapping("/api/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a tag")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag deleted"),
            @ApiResponse(responseCode = "404", description = "Tag not found")
    })
    public void deleteTag(
            @Parameter(description = "Tag ID") @PathVariable Long id) {
        metadataService.deleteTag(id);
    }
}
