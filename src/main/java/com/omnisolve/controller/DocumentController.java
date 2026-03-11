package com.omnisolve.controller;

import com.omnisolve.security.AuthenticationUtil;
import com.omnisolve.service.DocumentService;
import com.omnisolve.service.dto.DocumentRequest;
import com.omnisolve.service.dto.DocumentResponse;
import com.omnisolve.service.dto.DocumentStatsResponse;
import com.omnisolve.service.dto.DocumentVersionResponse;
import com.omnisolve.service.dto.DocumentAttentionResponse;
import com.omnisolve.service.dto.DocumentWorkflowStatsResponse;
import com.omnisolve.service.dto.UpcomingReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Manage controlled documents, workflow state, and S3-backed versions")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @Operation(summary = "List documents")
    public List<DocumentResponse> getAll() {
        return documentService.listDocuments();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get document statistics for dashboard")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public DocumentStatsResponse getStats() {
        return documentService.getStats();
    }

    @GetMapping("/attention")
    @Operation(summary = "Get documents requiring attention")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attention items retrieved successfully")
    })
    public DocumentAttentionResponse getAttention() {
        return documentService.getAttention();
    }

    @GetMapping("/reviews/upcoming")
    @Operation(summary = "Get documents with upcoming review dates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upcoming reviews retrieved successfully")
    })
    public List<UpcomingReviewResponse> getUpcomingReviews(
            @Parameter(description = "Number of days to look ahead")
            @RequestParam(defaultValue = "30") int days
    ) {
        return documentService.getUpcomingReviews(days);
    }

    @GetMapping("/workflow")
    @Operation(summary = "Get workflow distribution statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workflow statistics retrieved successfully")
    })
    public DocumentWorkflowStatsResponse getWorkflowStats() {
        return documentService.getWorkflowStats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a document by id")
    public DocumentResponse getById(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.getDocument(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a document")
    public DocumentResponse create(@RequestBody DocumentRequest request) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return documentService.create(request, userId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a document")
    public DocumentResponse update(
            @Parameter(description = "Document UUID") @PathVariable UUID id,
            @RequestBody DocumentRequest request
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return documentService.update(id, request, userId);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a document for approval")
    public DocumentResponse submit(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return documentService.submit(id, userId);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending document")
    public DocumentResponse approve(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return documentService.approve(id, userId);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending document")
    public DocumentResponse reject(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return documentService.reject(id, userId);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive an active document")
    public DocumentResponse archive(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return documentService.archive(id, userId);
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a new document version file")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Version uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(hidden = true)))
    })
    public DocumentVersionResponse uploadVersion(
            @Parameter(description = "Document UUID") @PathVariable UUID id,
            @Parameter(description = "Document file payload") @RequestPart("file") MultipartFile file
    ) {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        return documentService.uploadVersion(id, file, userId);
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get all versions for a document")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public List<DocumentVersionResponse> getVersions(
            @Parameter(description = "Document UUID") @PathVariable UUID id
    ) {
        return documentService.getDocumentVersions(id);
    }
}
