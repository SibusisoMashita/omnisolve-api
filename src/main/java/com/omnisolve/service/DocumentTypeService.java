package com.omnisolve.service;

import com.omnisolve.domain.DocumentType;
import com.omnisolve.repository.DocumentTypeRepository;
import com.omnisolve.service.dto.DocumentTypeRequest;
import com.omnisolve.service.dto.DocumentTypeResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DocumentTypeService {

    private final DocumentTypeRepository repository;

    public DocumentTypeService(DocumentTypeRepository repository) {
        this.repository = repository;
    }

    public List<DocumentTypeResponse> list() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentTypeResponse create(DocumentTypeRequest request) {
        DocumentType type = new DocumentType();
        type.setName(request.name());
        type.setDescription(request.description());
        type.setRequiresClauses(request.requiresClauses() != null ? request.requiresClauses() : false);
        return toResponse(repository.save(type));
    }

    @Transactional
    public DocumentTypeResponse update(Long id, DocumentTypeRequest request) {
        DocumentType type = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        type.setName(request.name());
        type.setDescription(request.description());
        type.setRequiresClauses(request.requiresClauses() != null ? request.requiresClauses() : false);
        return toResponse(repository.save(type));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
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

