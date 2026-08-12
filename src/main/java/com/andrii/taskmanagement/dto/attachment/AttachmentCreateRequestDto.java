package com.andrii.taskmanagement.dto.attachment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttachmentCreateRequestDto(
        @NotNull
        Long taskId,

        @NotBlank
        String dropboxFileId,

        @NotBlank
        String filename
) {
}
