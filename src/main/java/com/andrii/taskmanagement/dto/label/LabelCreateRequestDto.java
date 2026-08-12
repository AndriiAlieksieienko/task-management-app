package com.andrii.taskmanagement.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LabelCreateRequestDto(
        @NotNull
        Long projectId,

        @NotBlank
        String name,

        @NotBlank
        String color
) {
}
