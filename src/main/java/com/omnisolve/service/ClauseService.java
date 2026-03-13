package com.omnisolve.service;

import com.omnisolve.domain.Clause;
import com.omnisolve.domain.Document;
import com.omnisolve.domain.Standard;
import com.omnisolve.repository.ClauseRepository;
import com.omnisolve.repository.DocumentClauseLinkRepository;
import com.omnisolve.repository.DocumentRepository;
import com.omnisolve.repository.StandardRepository;
import com.omnisolve.service.dto.ClauseRequest;
import com.omnisolve.service.dto.ClauseResponse;
import com.omnisolve.service.dto.DocumentResponse;
import com.omnisolve.service.dto.StandardResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ClauseService {

    private static final Logger log = LoggerFactory.getLogger(ClauseService.class);

    private final ClauseRepository repository;
    private final StandardRepository standardRepository;
    private final DocumentRepository documentRepository;
    private final DocumentClauseLinkRepository documentClauseLinkRepository;

    public ClauseService(ClauseRepository repository, StandardRepository standardRepository,
            DocumentRepository documentRepository, DocumentClauseLinkRepository documentClauseLinkRepository) {
        this.repository = repository;
        this.standardRepository = standardRepository;
        this.documentRepository = documentRepository;
        this.documentClauseLinkRepository = documentClauseLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<ClauseResponse> list() {
        log.info("Fetching all clauses");
        List<Clause> clauses = repository.findAll();
        log.info("Retrieved {} clauses", clauses.size());
        return clauses.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StandardResponse> listStandards() {
        log.info("Fetching all standards");
        return standardRepository.findAll().stream()
                .map(s -> new StandardResponse(s.getId(), s.getCode(), s.getName(), s.getVersion(), s.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClauseResponse> listClausesByStandard(Long standardId) {
        log.info("Fetching clauses for standard: id={}", standardId);
        standardRepository.findById(standardId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Standard not found"));
        return repository.findByStandardIdOrderBySortOrderAsc(standardId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClauseResponse create(ClauseRequest request) {
        log.info("Creating clause: code={}, title={}, standardId={}", request.code(), request.title(), request.standardId());
        try {
            Standard standard = standardRepository.findById(request.standardId())
                    .orElseThrow(() -> {
                        log.warn("Standard not found: id={}", request.standardId());
                        return new ResponseStatusException(NOT_FOUND, "Standard not found");
                    });

            Clause clause = new Clause();
            clause.setStandard(standard);
            clause.setCode(request.code());
            clause.setTitle(request.title());
            clause.setDescription(request.description());
            clause.setParentCode(request.parentCode());
            clause.setLevel(request.level() != null ? request.level() : 1);
            clause.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
            
            Clause saved = repository.save(clause);
            log.info("Clause created successfully: id={}, code={}", saved.getId(), saved.getCode());
            return toResponse(saved);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create clause: code={}, error={}", request.code(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public ClauseResponse update(Long id, ClauseRequest request) {
        log.info("Updating clause: id={}, code={}, title={}", id, request.code(), request.title());
        try {
            Clause clause = repository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Clause not found: id={}", id);
                        return new ResponseStatusException(NOT_FOUND, "Clause not found");
                    });

            if (request.standardId() != null) {
                Standard standard = standardRepository.findById(request.standardId())
                        .orElseThrow(() -> {
                            log.warn("Standard not found: id={}", request.standardId());
                            return new ResponseStatusException(NOT_FOUND, "Standard not found");
                        });
                clause.setStandard(standard);
            }

            clause.setCode(request.code());
            clause.setTitle(request.title());
            clause.setDescription(request.description());
            clause.setParentCode(request.parentCode());
            if (request.level() != null) {
                clause.setLevel(request.level());
            }
            if (request.sortOrder() != null) {
                clause.setSortOrder(request.sortOrder());
            }
            
            Clause saved = repository.save(clause);
            log.info("Clause updated successfully: id={}, code={}", saved.getId(), saved.getCode());
            return toResponse(saved);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update clause: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByClause(Long clauseId) {
        log.info("Fetching documents for clause: id={}", clauseId);
        
        // Verify clause exists
        repository.findById(clauseId)
                .orElseThrow(() -> {
                    log.warn("Clause not found: id={}", clauseId);
                    return new ResponseStatusException(NOT_FOUND, "Clause not found");
                });
        
        List<Document> documents = documentRepository.findByClauseId(clauseId);
        log.info("Found {} documents for clause id={}", documents.size(), clauseId);
        
        return documents.stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    private ClauseResponse toResponse(Clause clause) {
        Standard standard = clause.getStandard();
        return new ClauseResponse(
                clause.getId(),
                standard.getId(),
                standard.getCode(),
                standard.getName(),
                clause.getCode(),
                clause.getTitle(),
                clause.getDescription(),
                clause.getParentCode(),
                clause.getLevel(),
                clause.getSortOrder(),
                clause.getCreatedAt()
        );
    }

    private DocumentResponse toDocumentResponse(Document doc) {
        List<Long> clauseIds = documentClauseLinkRepository.findClauseIdsByDocumentId(doc.getId());
        return new DocumentResponse(
                doc.getId(),
                doc.getDocumentNumber(),
                doc.getTitle(),
                doc.getSummary(),
                doc.getType().getId(),
                doc.getType().getName(),
                doc.getType().getRequiresClauses(),
                doc.getDepartment() != null ? doc.getDepartment().getId() : null,
                doc.getDepartment() != null ? doc.getDepartment().getName() : null,
                doc.getStatus().getId(),
                doc.getStatus().getName(),
                doc.getOwnerId(),
                doc.getCreatedBy(),
                doc.getNextReviewAt(),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                clauseIds
        );
    }
}
