package com.omnisolve.controller;

import com.omnisolve.security.AuthenticationUtil;
import com.omnisolve.service.IncidentService;
import com.omnisolve.service.dto.*;
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
@RequestMapping("/api/incidents")
@Tag(name = "Incidents", description = "Manage incidents, investigations, and corrective actions")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @Operation(summary = "List incidents with optional filters and pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidents retrieved successfully")
    })
    public Page<IncidentResponse> listIncidents(
            @Parameter(description = "Filter by status ID")
            @RequestParam(required = false) Long status,

            @Parameter(description = "Filter by severity ID")
            @RequestParam(required = false) Long severity,

            @Parameter(description = "Filter by department ID")
            @RequestParam(required = false) Long department,

            @Parameter(description = "Filter by site ID")
            @RequestParam(required = false) Long site,

            @Parameter(description = "Search in title and incident number")
            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return incidentService.listIncidents(status, severity, department, site, search, pageable);
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get incident detail including attachments, investigation, actions, and comments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident detail retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentDetailResponse getIncident(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId
    ) {
        return incidentService.getIncident(incidentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new incident")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incident created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentResponse createIncident(
            @Valid @RequestBody IncidentRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.createIncident(request, userId);
    }

    @PutMapping("/{incidentId}")
    @Operation(summary = "Update an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident updated successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentResponse updateIncident(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId,
            @RequestBody IncidentUpdateRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.updateIncident(incidentId, request, userId);
    }

    @PatchMapping("/{incidentId}/status")
    @Operation(summary = "Change incident status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed successfully"),
            @ApiResponse(responseCode = "404", description = "Incident or status not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentResponse changeStatus(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId,
            @Valid @RequestBody IncidentStatusChangeRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.changeStatus(incidentId, request.statusId(), userId);
    }

    @PatchMapping("/{incidentId}/assign")
    @Operation(summary = "Assign investigator to incident")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Investigator assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentResponse assignInvestigator(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId,
            @Valid @RequestBody IncidentAssignRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.assignInvestigator(incidentId, request.investigatorId(), userId);
    }

    @PatchMapping("/{incidentId}/close")
    @Operation(summary = "Close an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident closed successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentResponse closeIncident(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.closeIncident(incidentId, userId);
    }

    @PostMapping("/{incidentId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an attachment to an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attachment uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentAttachmentResponse uploadAttachment(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId,
            @Parameter(description = "File to upload") @RequestPart("file") MultipartFile file
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.uploadAttachment(incidentId, file, userId);
    }

    @GetMapping("/{incidentId}/attachments")
    @Operation(summary = "List all attachments for an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public List<IncidentAttachmentResponse> listAttachments(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId
    ) {
        return incidentService.listAttachments(incidentId);
    }

    @PostMapping("/{incidentId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment added successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentCommentResponse addComment(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId,
            @Valid @RequestBody IncidentCommentRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.addComment(incidentId, request.comment(), userId);
    }

    @GetMapping("/{incidentId}/comments")
    @Operation(summary = "List all comments for an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public List<IncidentCommentResponse> listComments(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId
    ) {
        return incidentService.listComments(incidentId);
    }

    @PostMapping("/{incidentId}/investigation")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add investigation details to an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Investigation added successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentInvestigationResponse addInvestigation(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId,
            @Valid @RequestBody IncidentInvestigationRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.addInvestigation(incidentId, request, userId);
    }

    @PostMapping("/{incidentId}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a corrective action to an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Corrective action added successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentActionResponse addAction(
            @Parameter(description = "Incident UUID") @PathVariable UUID incidentId,
            @Valid @RequestBody IncidentActionRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.addAction(incidentId, request, userId);
    }

    @PatchMapping("/actions/{actionId}")
    @Operation(summary = "Update a corrective action status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Corrective action updated successfully"),
            @ApiResponse(responseCode = "404", description = "Action not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public IncidentActionResponse updateAction(
            @Parameter(description = "Action ID") @PathVariable Long actionId,
            @RequestBody IncidentActionUpdateRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return incidentService.updateAction(actionId, request, userId);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get incident dashboard metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully")
    })
    public IncidentDashboardResponse getDashboard() {
        return incidentService.getDashboard();
    }
}
