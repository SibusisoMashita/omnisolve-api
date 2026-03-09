package com.omnisolve.controller;

import com.omnisolve.service.DocumentTypeService;
import com.omnisolve.service.dto.DocumentTypeRequest;
import com.omnisolve.service.dto.DocumentTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document-types")
@Tag(name = "Document Types", description = "Manage business document type catalog")
public class DocumentTypeController {

    private final DocumentTypeService service;

    public DocumentTypeController(DocumentTypeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List document types")
    public List<DocumentTypeResponse> getAll() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a document type")
    public DocumentTypeResponse create(@RequestBody DocumentTypeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a document type")
    public DocumentTypeResponse update(@Parameter(description = "Document type id") @PathVariable Long id, @RequestBody DocumentTypeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document type")
    public void delete(@Parameter(description = "Document type id") @PathVariable Long id) {
        service.delete(id);
    }
}
