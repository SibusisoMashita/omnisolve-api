package com.omnisolve.service.dto;

public record ClauseRequest(
        Long standardId,
        String code,
        String title,
        String description,
        String parentCode,
        Integer level,
        Integer sortOrder
) {
}

