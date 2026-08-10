package com.andrii.taskmanagement.dto.task;

import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record TaskResponseDto(
        Long id,
        Long projectId,
        Long assigneeId,
        Long createdById,
        String name,
        String description,
        Priority priority,
        TaskStatus status,
        LocalDate dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<Long> labelsId
) {
}
