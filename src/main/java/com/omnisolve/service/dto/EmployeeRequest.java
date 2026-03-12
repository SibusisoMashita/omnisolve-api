package com.omnisolve.service.dto;

public record EmployeeRequest(
        String email,
        String firstName,
        String lastName,
        Long roleId,
        Long departmentId,
        Long siteId
) {
}
