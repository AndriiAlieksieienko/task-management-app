package com.andrii.taskmanagement.dto.label;

import java.time.LocalDateTime;

public record LabelResponseDto(
        Long id,
        Long projectId,
        String name,
        String color,
        LocalDateTime createdAt
) {
}
