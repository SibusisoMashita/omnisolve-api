package com.omnisolve.controller;

import com.omnisolve.service.EmployeeService;
import com.omnisolve.service.dto.EmployeeRequest;
import com.omnisolve.service.dto.EmployeeResponse;
import com.omnisolve.service.dto.EmployeeStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Manage employees with multi-organisation support")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List employees", description = "Returns all employees in the authenticated user's organisation")
    public List<EmployeeResponse> getAll() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an employee", description = "Creates a new employee in Cognito and the local database")
    public EmployeeResponse create(@RequestBody EmployeeRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update an employee", description = "Updates employee details in the local database")
    public EmployeeResponse update(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @RequestBody EmployeeRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update employee status", description = "Updates employee status and enables/disables the user in Cognito")
    public EmployeeResponse updateStatus(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @RequestBody EmployeeStatusRequest request
    ) {
        return service.updateStatus(id, request.status());
    }
}
