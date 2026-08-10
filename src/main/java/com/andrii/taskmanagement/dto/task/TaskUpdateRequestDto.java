package com.andrii.taskmanagement.dto.task;

import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.Set;

public record TaskUpdateRequestDto(
        @NotNull
        Long assigneeId,

        @NotBlank
        String name,

        String description,

        @NotNull
        Priority priority,

        @NotNull
        TaskStatus status,

        @NotNull
        LocalTime dueDate,

        @NotNull
        Set<Long> labelsId
) {
}
