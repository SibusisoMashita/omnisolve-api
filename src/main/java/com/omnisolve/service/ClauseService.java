package com.omnisolve.service;

import com.omnisolve.domain.Clause;
import com.omnisolve.repository.ClauseRepository;
import com.omnisolve.service.dto.ClauseRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ClauseService {

    private final ClauseRepository repository;

    public ClauseService(ClauseRepository repository) {
        this.repository = repository;
    }

    public List<Clause> list() {
        return repository.findAll();
    }

    @Transactional
    public Clause create(ClauseRequest request) {
        Clause clause = new Clause();
        clause.setCode(request.code());
        clause.setTitle(request.title());
        clause.setDescription(request.description());
        return repository.save(clause);
    }

    @Transactional
    public Clause update(Long id, ClauseRequest request) {
        Clause clause = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Clause not found"));
        clause.setCode(request.code());
        clause.setTitle(request.title());
        clause.setDescription(request.description());
        return repository.save(clause);
    }
}

