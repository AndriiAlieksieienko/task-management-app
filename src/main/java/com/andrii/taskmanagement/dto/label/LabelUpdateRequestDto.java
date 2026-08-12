package com.andrii.taskmanagement.dto.label;

import jakarta.validation.constraints.NotBlank;

public record LabelUpdateRequestDto(
        @NotBlank
        String name,

        @NotBlank
        String color
) {
}
