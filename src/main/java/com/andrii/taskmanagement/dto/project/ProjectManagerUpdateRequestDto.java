package com.andrii.taskmanagement.dto.project;

import jakarta.validation.constraints.NotNull;

public record ProjectManagerUpdateRequestDto(
        @NotNull
        Long projectManagerId
) {
}
