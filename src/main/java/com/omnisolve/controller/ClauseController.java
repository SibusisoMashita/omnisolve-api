package com.omnisolve.controller;

import com.omnisolve.domain.Clause;
import com.omnisolve.service.ClauseService;
import com.omnisolve.service.dto.ClauseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clauses")
@Tag(name = "Clauses", description = "Manage ISO clauses linked to documents")
public class ClauseController {

    private final ClauseService service;

    public ClauseController(ClauseService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List clauses")
    public List<Clause> getAll() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a clause")
    public Clause create(@RequestBody ClauseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a clause")
    public Clause update(@Parameter(description = "Clause id") @PathVariable Long id, @RequestBody ClauseRequest request) {
        return service.update(id, request);
    }
}
