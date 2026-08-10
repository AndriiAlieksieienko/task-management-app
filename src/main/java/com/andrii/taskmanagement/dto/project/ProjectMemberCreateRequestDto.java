package com.andrii.taskmanagement.dto.project;

import jakarta.validation.constraints.NotNull;

public record ProjectMemberCreateRequestDto(
        @NotNull
        Long userId
) {
}
