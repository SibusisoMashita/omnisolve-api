package com.omnisolve.service;

import com.omnisolve.domain.DocumentType;
import com.omnisolve.repository.DocumentTypeRepository;
import com.omnisolve.service.dto.DocumentTypeRequest;
import com.omnisolve.service.dto.DocumentTypeResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DocumentTypeService {

    private static final Logger log = LoggerFactory.getLogger(DocumentTypeService.class);

    private final DocumentTypeRepository repository;

    public DocumentTypeService(DocumentTypeRepository repository) {
        this.repository = repository;
    }

    public List<DocumentTypeResponse> list() {
        log.info("Fetching all document types");
        List<DocumentTypeResponse> types = repository.findAll().stream()
                .map(this::toResponse)
                .toList();
        log.info("Retrieved {} document types", types.size());
        return types;
    }

    @Transactional
    public DocumentTypeResponse create(DocumentTypeRequest request) {
        log.info("Creating document type: name={}, requiresClauses={}", request.name(), request.requiresClauses());
        try {
            DocumentType type = new DocumentType();
            type.setName(request.name());
            type.setDescription(request.description());
            type.setRequiresClauses(request.requiresClauses() != null ? request.requiresClauses() : false);
            DocumentType saved = repository.save(type);
            log.info("Document type created successfully: id={}, name={}", saved.getId(), saved.getName());
            return toResponse(saved);
        } catch (Exception e) {
            log.error("Failed to create document type: name={}, error={}", request.name(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public DocumentTypeResponse update(Long id, DocumentTypeRequest request) {
        log.info("Updating document type: id={}, name={}", id, request.name());
        try {
            DocumentType type = repository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Document type not found: id={}", id);
                        return new ResponseStatusException(NOT_FOUND, "Document type not found");
                    });
            type.setName(request.name());
            type.setDescription(request.description());
            type.setRequiresClauses(request.requiresClauses() != null ? request.requiresClauses() : false);
            DocumentType saved = repository.save(type);
            log.info("Document type updated successfully: id={}, name={}", saved.getId(), saved.getName());
            return toResponse(saved);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update document type: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting document type: id={}", id);
        try {
            repository.deleteById(id);
            log.info("Document type deleted successfully: id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete document type: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    private DocumentTypeResponse toResponse(DocumentType type) {
        return new DocumentTypeResponse(
                type.getId(),
                type.getName(),
                type.getDescription(),
                type.getRequiresClauses()
        );
    }
}

