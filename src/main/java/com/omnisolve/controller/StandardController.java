package com.omnisolve.controller;

import com.omnisolve.service.ClauseService;
import com.omnisolve.service.dto.ClauseResponse;
import com.omnisolve.service.dto.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/standards")
@Tag(name = "Standards", description = "ISO standards and their clauses")
public class StandardController {

    private final ClauseService clauseService;

    public StandardController(ClauseService clauseService) {
        this.clauseService = clauseService;
    }

    @GetMapping
    @Operation(summary = "List all compliance standards")
    public List<StandardResponse> getAll() {
        return clauseService.listStandards();
    }

    @GetMapping("/{standardId}/clauses")
    @Operation(summary = "List clauses for a standard, ordered for hierarchical display")
    public List<ClauseResponse> getClausesByStandard(
            @Parameter(description = "Standard ID") @PathVariable Long standardId
    ) {
        return clauseService.listClausesByStandard(standardId);
    }
}