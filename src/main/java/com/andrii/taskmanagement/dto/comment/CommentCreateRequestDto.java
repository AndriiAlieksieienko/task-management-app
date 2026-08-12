package com.andrii.taskmanagement.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequestDto(
        @NotNull
        Long taskId,

        @NotBlank
        String text
) {
}
