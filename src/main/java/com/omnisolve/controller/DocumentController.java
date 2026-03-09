package com.omnisolve.controller;

import com.omnisolve.service.DocumentService;
import com.omnisolve.service.dto.DocumentRequest;
import com.omnisolve.service.dto.DocumentResponse;
import com.omnisolve.service.dto.DocumentVersionResponse;
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

    @GetMapping("/{id}")
    @Operation(summary = "Get a document by id")
    public DocumentResponse getById(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.getDocument(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a document")
    public DocumentResponse create(@RequestBody DocumentRequest request) {
        return documentService.create(request, "test-user");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a document")
    public DocumentResponse update(
            @Parameter(description = "Document UUID") @PathVariable UUID id,
            @RequestBody DocumentRequest request
    ) {
        return documentService.update(id, request, "test-user");
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a document for approval")
    public DocumentResponse submit(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.submit(id, "test-user");
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending document")
    public DocumentResponse approve(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.approve(id, "test-user");
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending document")
    public DocumentResponse reject(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.reject(id, "test-user");
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive an active document")
    public DocumentResponse archive(@Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.archive(id, "test-user");
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
        return documentService.uploadVersion(id, file, "test-user");
    }
}
