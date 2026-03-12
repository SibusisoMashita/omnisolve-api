package com.omnisolve.service.dto;

import java.time.OffsetDateTime;

public record EmployeeResponse(
        Long id,
        String cognitoSub,
        String cognitoUsername,
        String email,
        String firstName,
        String lastName,
        Long roleId,
        String roleName,
        Long departmentId,
        String departmentName,
        Long organisationId,
        String organisationName,
        Long siteId,
        String siteName,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
