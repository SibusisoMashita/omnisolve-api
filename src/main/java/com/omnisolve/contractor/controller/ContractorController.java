package com.omnisolve.contractor.controller;

import com.omnisolve.contractor.service.ContractorDocumentService;
import com.omnisolve.contractor.service.ContractorService;
import com.omnisolve.contractor.service.ContractorWorkerService;
import com.omnisolve.contractor.service.dto.*;
import com.omnisolve.security.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Contractors", description = "Manage contractors, workers, documents and compliance")
public class ContractorController {

    private final ContractorService contractorService;
    private final ContractorWorkerService workerService;
    private final ContractorDocumentService documentService;

    public ContractorController(
            ContractorService contractorService,
            ContractorWorkerService workerService,
            ContractorDocumentService documentService) {
        this.contractorService = contractorService;
        this.workerService = workerService;
        this.documentService = documentService;
    }

    // ── Contractors ──────────────────────────────────────────────────────────

    @GetMapping("/api/contractors")
    @Operation(summary = "List all contractors for the current organisation")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Contractors retrieved successfully"))
    public List<ContractorResponse> listContractors() {
        return contractorService.listContractorsByOrganisation();
    }

    @PostMapping("/api/contractors")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new contractor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contractor created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(hidden = true)))
    })
    public ContractorResponse createContractor(@Valid @RequestBody ContractorRequest request) {
        return contractorService.createContractor(request);
    }

    @GetMapping("/api/contractors/{id}")
    @Operation(summary = "Get a contractor by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contractor retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Contractor not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ContractorResponse getContractor(
            @Parameter(description = "Contractor UUID") @PathVariable UUID id) {
        return contractorService.getContractor(id);
    }

    @PutMapping("/api/contractors/{id}")
    @Operation(summary = "Update a contractor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contractor updated successfully"),
            @ApiResponse(responseCode = "404", description = "Contractor not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ContractorResponse updateContractor(
            @Parameter(description = "Contractor UUID") @PathVariable UUID id,
            @RequestBody ContractorRequest request) {
        return contractorService.updateContractor(id, request);
    }

    @DeleteMapping("/api/contractors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a contractor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contractor deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Contractor not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public void deleteContractor(
            @Parameter(description = "Contractor UUID") @PathVariable UUID id) {
        contractorService.deleteContractor(id);
    }

    @GetMapping("/api/contractors/compliance")
    @Operation(summary = "Get compliance status for all contractors")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Compliance data retrieved successfully"))
    public List<ContractorComplianceResponse> getCompliance() {
        return contractorService.getComplianceByOrganisation();
    }

    // ── Workers ───────────────────────────────────────────────────────────────

    @GetMapping("/api/contractors/{contractorId}/workers")
    @Operation(summary = "List all workers for a contractor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workers retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Contractor not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public List<ContractorWorkerResponse> listWorkers(
            @Parameter(description = "Contractor UUID") @PathVariable UUID contractorId) {
        return workerService.listWorkers(contractorId);
    }

    @PostMapping("/api/contractors/{contractorId}/workers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a worker to a contractor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Worker added successfully"),
            @ApiResponse(responseCode = "404", description = "Contractor not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ContractorWorkerResponse addWorker(
            @Parameter(description = "Contractor UUID") @PathVariable UUID contractorId,
            @RequestBody ContractorWorkerRequest request) {
        return workerService.addWorker(contractorId, request);
    }

    @PutMapping("/api/workers/{workerId}")
    @Operation(summary = "Update a worker")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worker updated successfully"),
            @ApiResponse(responseCode = "404", description = "Worker not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ContractorWorkerResponse updateWorker(
            @Parameter(description = "Worker UUID") @PathVariable UUID workerId,
            @RequestBody ContractorWorkerRequest request) {
        return workerService.updateWorker(workerId, request);
    }

    @DeleteMapping("/api/workers/{workerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a worker")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Worker deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Worker not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public void deleteWorker(
            @Parameter(description = "Worker UUID") @PathVariable UUID workerId) {
        workerService.deleteWorker(workerId);
    }

    // ── Documents ─────────────────────────────────────────────────────────────

    @GetMapping("/api/contractors/{contractorId}/documents")
    @Operation(summary = "List all documents for a contractor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Contractor not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public List<ContractorDocumentResponse> listDocuments(
            @Parameter(description = "Contractor UUID") @PathVariable UUID contractorId) {
        return documentService.listDocuments(contractorId);
    }

    @PostMapping("/api/contractors/{contractorId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a compliance document for a contractor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Contractor not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ContractorDocumentResponse uploadDocument(
            @Parameter(description = "Contractor UUID") @PathVariable UUID contractorId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "documentTypeId", required = false) String documentTypeId,
            @RequestPart(value = "issuedAt", required = false) String issuedAt,
            @RequestPart(value = "expiryDate", required = false) String expiryDate) {

        String userId = AuthenticationUtil.getAuthenticatedUserId();
        ContractorDocumentRequest request = new ContractorDocumentRequest(
                documentTypeId != null ? Long.parseLong(documentTypeId) : null,
                issuedAt != null ? java.time.LocalDate.parse(issuedAt) : null,
                expiryDate != null ? java.time.LocalDate.parse(expiryDate) : null);

        return documentService.uploadDocument(contractorId, file, request, userId);
    }

    @DeleteMapping("/api/contractor-documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a contractor document")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public void deleteDocument(
            @Parameter(description = "Document ID") @PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
    }
}
