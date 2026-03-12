package com.omnisolve.controller;

import com.omnisolve.service.RoleService;
import com.omnisolve.service.dto.RoleRequest;
import com.omnisolve.service.dto.RoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Manage organisation-scoped roles and permissions")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "List roles", description = "Returns all roles in the authenticated user's organisation")
    public List<RoleResponse> getAll() {
        return roleService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create role", description = "Creates a role and assigns permissions")
    public RoleResponse create(@RequestBody RoleRequest request) {
        return roleService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update role", description = "Updates role details and assigned permissions")
    public RoleResponse update(
            @Parameter(description = "Role ID") @PathVariable Long id,
            @RequestBody RoleRequest request
    ) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete role", description = "Deletes a role in the authenticated user's organisation")
    public void delete(@Parameter(description = "Role ID") @PathVariable Long id) {
        roleService.delete(id);
    }
}

