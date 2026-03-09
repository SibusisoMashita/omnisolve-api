package com.omnisolve.service;

import com.omnisolve.domain.DocumentType;
import com.omnisolve.repository.DocumentTypeRepository;
import com.omnisolve.service.dto.DocumentTypeRequest;
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

    public List<DocumentType> list() {
        return repository.findAll();
    }

    @Transactional
    public DocumentType create(DocumentTypeRequest request) {
        DocumentType type = new DocumentType();
        type.setName(request.name());
        type.setDescription(request.description());
        return repository.save(type);
    }

    @Transactional
    public DocumentType update(Long id, DocumentTypeRequest request) {
        DocumentType type = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        type.setName(request.name());
        type.setDescription(request.description());
        return repository.save(type);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}

