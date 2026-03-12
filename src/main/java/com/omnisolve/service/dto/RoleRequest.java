package com.omnisolve.service.dto;

import java.util.List;

public record RoleRequest(
        String name,
        String description,
        List<String> permissions
) {
}

