package com.omnisolve.service;

import com.omnisolve.domain.Clause;
import com.omnisolve.repository.ClauseRepository;
import com.omnisolve.service.dto.ClauseRequest;
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

    public ClauseService(ClauseRepository repository) {
        this.repository = repository;
    }

    public List<Clause> list() {
        log.info("Fetching all clauses");
        List<Clause> clauses = repository.findAll();
        log.info("Retrieved {} clauses", clauses.size());
        return clauses;
    }

    @Transactional
    public Clause create(ClauseRequest request) {
        log.info("Creating clause: code={}, title={}", request.code(), request.title());
        try {
            Clause clause = new Clause();
            clause.setCode(request.code());
            clause.setTitle(request.title());
            clause.setDescription(request.description());
            Clause saved = repository.save(clause);
            log.info("Clause created successfully: id={}, code={}", saved.getId(), saved.getCode());
            return saved;
        } catch (Exception e) {
            log.error("Failed to create clause: code={}, error={}", request.code(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public Clause update(Long id, ClauseRequest request) {
        log.info("Updating clause: id={}, code={}, title={}", id, request.code(), request.title());
        try {
            Clause clause = repository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Clause not found: id={}", id);
                        return new ResponseStatusException(NOT_FOUND, "Clause not found");
                    });
            clause.setCode(request.code());
            clause.setTitle(request.title());
            clause.setDescription(request.description());
            Clause saved = repository.save(clause);
            log.info("Clause updated successfully: id={}, code={}", saved.getId(), saved.getCode());
            return saved;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update clause: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }
}

