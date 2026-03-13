package com.omnisolve.assurance.controller;

import com.omnisolve.assurance.dto.ChecklistTemplateResponse;
import com.omnisolve.assurance.dto.InspectionAttachmentResponse;
import com.omnisolve.assurance.dto.InspectionDashboardResponse;
import com.omnisolve.assurance.dto.InspectionDetailResponse;
import com.omnisolve.assurance.dto.InspectionFindingRequest;
import com.omnisolve.assurance.dto.InspectionFindingResponse;
import com.omnisolve.assurance.dto.InspectionItemRequest;
import com.omnisolve.assurance.dto.InspectionItemResponse;
import com.omnisolve.assurance.dto.InspectionRequest;
import com.omnisolve.assurance.dto.InspectionResponse;
import com.omnisolve.assurance.service.InspectionService;
import com.omnisolve.security.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assurance/inspections")
@Tag(name = "Inspections", description = "Manage asset inspections, checklists, findings and photo evidence")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @GetMapping
    @Operation(summary = "List inspections with optional filters and pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspections retrieved successfully")
    })
    public Page<InspectionResponse> listInspections(
            @Parameter(description = "Filter by status (SCHEDULED, IN_PROGRESS, COMPLETED)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Search by title or inspection number")
            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return inspectionService.listInspections(status, search, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new inspection")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inspection created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public InspectionResponse createInspection(@Valid @RequestBody InspectionRequest request) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return inspectionService.createInspection(request, userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inspection detail including items, findings and attachments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspection retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public InspectionDetailResponse getInspection(
            @Parameter(description = "Inspection UUID") @PathVariable UUID id) {
        return inspectionService.getInspection(id);
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Start an inspection")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspection started"),
            @ApiResponse(responseCode = "400", description = "Inspection not in SCHEDULED status", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public InspectionResponse startInspection(
            @Parameter(description = "Inspection UUID") @PathVariable UUID id) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return inspectionService.startInspection(id, userId);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete and submit an inspection")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspection completed"),
            @ApiResponse(responseCode = "400", description = "Inspection not IN_PROGRESS", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public InspectionResponse completeInspection(
            @Parameter(description = "Inspection UUID") @PathVariable UUID id) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return inspectionService.submitInspection(id, userId);
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a checklist item result")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public InspectionItemResponse addItem(
            @Parameter(description = "Inspection UUID") @PathVariable UUID id,
            @Valid @RequestBody InspectionItemRequest request) {
        return inspectionService.addInspectionItem(id, request);
    }

    @PostMapping("/{id}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a finding to an inspection")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Finding added successfully"),
            @ApiResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public InspectionFindingResponse addFinding(
            @Parameter(description = "Inspection UUID") @PathVariable UUID id,
            @Valid @RequestBody InspectionFindingRequest request) {
        return inspectionService.addFinding(id, request);
    }

    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a photo or file attachment to an inspection")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attachment uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public InspectionAttachmentResponse uploadAttachment(
            @Parameter(description = "Inspection UUID") @PathVariable UUID id,
            @Parameter(description = "Photo or file to upload") @RequestPart("file") MultipartFile file) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return inspectionService.uploadInspectionAttachment(id, file, userId);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get inspection dashboard metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully")
    })
    public InspectionDashboardResponse getDashboard() {
        return inspectionService.getDashboard();
    }

    @GetMapping("/checklists")
    @Operation(summary = "List all checklist templates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checklists retrieved successfully")
    })
    public List<ChecklistTemplateResponse> listChecklistTemplates() {
        return inspectionService.listChecklistTemplates();
    }
}
