package com.andrii.taskmanagement.dto.project;

import com.andrii.taskmanagement.model.ProjectStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponseDto(
        Long id,
        String name,
        String description,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Long createdById,
        Long projectManagerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
