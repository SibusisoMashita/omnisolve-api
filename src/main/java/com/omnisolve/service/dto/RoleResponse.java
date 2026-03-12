package com.omnisolve.service.dto;

import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        List<String> permissions,
        long userCount
) {
}

