package com.omnisolve.risk.controller;

import com.omnisolve.risk.service.RiskAttachmentService;
import com.omnisolve.risk.service.RiskControlService;
import com.omnisolve.risk.service.RiskMetadataService;
import com.omnisolve.risk.service.RiskService;
import com.omnisolve.risk.service.dto.*;
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
@Tag(name = "Risk", description = "Manage the risk register, controls, and attachments")
public class RiskController {

    private final RiskService riskService;
    private final RiskControlService controlService;
    private final RiskAttachmentService attachmentService;
    private final RiskMetadataService metadataService;

    public RiskController(
            RiskService riskService,
            RiskControlService controlService,
            RiskAttachmentService attachmentService,
            RiskMetadataService metadataService) {
        this.riskService = riskService;
        this.controlService = controlService;
        this.attachmentService = attachmentService;
        this.metadataService = metadataService;
    }

    // ── Metadata ──────────────────────────────────────────────────────────────

    @GetMapping("/api/risks/metadata")
    @Operation(summary = "Get risk form metadata (categories, severities, likelihoods)")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Metadata retrieved successfully"))
    public RiskMetadataResponse getMetadata() {
        return metadataService.getMetadata();
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/api/risks/dashboard")
    @Operation(summary = "Get risk dashboard summary metrics")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully"))
    public RiskDashboardResponse getDashboard() {
        return riskService.getDashboard();
    }

    // ── Risk CRUD ─────────────────────────────────────────────────────────────

    @GetMapping("/api/risks")
    @Operation(summary = "List risks with optional filters and pagination")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Risks retrieved successfully"))
    public Page<RiskDTO> listRisks(
            @Parameter(description = "Filter by category ID")
            @RequestParam(required = false) Long category,

            @Parameter(description = "Filter by status (OPEN, MITIGATED, ACCEPTED, CLOSED)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Search by title")
            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return riskService.listRisks(category, status, search, pageable);
    }

    @PostMapping("/api/risks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new risk")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Risk created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(hidden = true)))
    })
    public RiskDTO createRisk(@Valid @RequestBody RiskCreateRequest request) {
        return riskService.createRisk(request);
    }

    @GetMapping("/api/risks/{riskId}")
    @Operation(summary = "Get a risk by ID including controls and attachments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Risk retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Risk not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public RiskDTO getRisk(
            @Parameter(description = "Risk UUID") @PathVariable UUID riskId
    ) {
        return riskService.getRisk(riskId);
    }

    @PutMapping("/api/risks/{riskId}")
    @Operation(summary = "Update a risk")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Risk updated successfully"),
            @ApiResponse(responseCode = "404", description = "Risk not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public RiskDTO updateRisk(
            @Parameter(description = "Risk UUID") @PathVariable UUID riskId,
            @RequestBody RiskUpdateRequest request
    ) {
        return riskService.updateRisk(riskId, request);
    }

    @DeleteMapping("/api/risks/{riskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a risk")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Risk deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Risk not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public void deleteRisk(
            @Parameter(description = "Risk UUID") @PathVariable UUID riskId
    ) {
        riskService.deleteRisk(riskId);
    }

    // ── Risk Controls ─────────────────────────────────────────────────────────

    @GetMapping("/api/risks/{riskId}/controls")
    @Operation(summary = "List all controls for a risk")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Controls retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Risk not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public List<RiskControlDTO> listControls(
            @Parameter(description = "Risk UUID") @PathVariable UUID riskId
    ) {
        return controlService.listControls(riskId);
    }

    @PostMapping("/api/risks/{riskId}/controls")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a control measure to a risk")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Control added successfully"),
            @ApiResponse(responseCode = "404", description = "Risk not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public RiskControlDTO addControl(
            @Parameter(description = "Risk UUID") @PathVariable UUID riskId,
            @Valid @RequestBody RiskControlRequest request
    ) {
        return controlService.addControl(riskId, request);
    }

    @PutMapping("/api/risk-controls/{controlId}")
    @Operation(summary = "Update a risk control")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Control updated successfully"),
            @ApiResponse(responseCode = "404", description = "Control not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public RiskControlDTO updateControl(
            @Parameter(description = "Control ID") @PathVariable Long controlId,
            @RequestBody RiskControlUpdateRequest request
    ) {
        return controlService.updateControl(controlId, request);
    }

    @DeleteMapping("/api/risk-controls/{controlId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a risk control")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Control deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Control not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public void deleteControl(
            @Parameter(description = "Control ID") @PathVariable Long controlId
    ) {
        controlService.deleteControl(controlId);
    }

    // ── Risk Attachments ──────────────────────────────────────────────────────

    @GetMapping("/api/risks/{riskId}/attachments")
    @Operation(summary = "List all attachments for a risk")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Risk not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public List<RiskAttachmentDTO> listAttachments(
            @Parameter(description = "Risk UUID") @PathVariable UUID riskId
    ) {
        return attachmentService.listAttachments(riskId);
    }

    @PostMapping("/api/risks/{riskId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an attachment to a risk")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attachment uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Risk not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public RiskAttachmentDTO uploadAttachment(
            @Parameter(description = "Risk UUID") @PathVariable UUID riskId,
            @Parameter(description = "File to upload") @RequestPart("file") MultipartFile file
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return attachmentService.uploadAttachment(riskId, file, userId);
    }

    @DeleteMapping("/api/risk-attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a risk attachment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attachment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Attachment not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public void deleteAttachment(
            @Parameter(description = "Attachment ID") @PathVariable Long attachmentId
    ) {
        attachmentService.deleteAttachment(attachmentId);
    }
}
