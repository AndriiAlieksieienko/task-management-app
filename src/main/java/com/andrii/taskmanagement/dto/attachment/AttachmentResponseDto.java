package com.andrii.taskmanagement.dto.attachment;

import java.time.LocalDateTime;

public record AttachmentResponseDto(
        Long id,
        Long taskId,
        Long uploadedById,
        String dropboxFileId,
        String filename,
        LocalDateTime uploadDate
) {
}
