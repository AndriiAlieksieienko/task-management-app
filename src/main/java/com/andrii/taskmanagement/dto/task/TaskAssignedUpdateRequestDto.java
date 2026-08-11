package com.andrii.taskmanagement.dto.task;

import com.andrii.taskmanagement.model.TaskStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TaskAssignedUpdateRequestDto(
        String description,

        @NotNull
        TaskStatus status,

        LocalDate dueDate
) {
}
