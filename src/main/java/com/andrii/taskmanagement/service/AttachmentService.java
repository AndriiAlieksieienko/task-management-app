package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.attachment.AttachmentCreateRequestDto;
import com.andrii.taskmanagement.dto.attachment.AttachmentResponseDto;
import java.util.List;

public interface AttachmentService {
    AttachmentResponseDto save(
            AttachmentCreateRequestDto requestDto,
            String userEmail
    );

    List<AttachmentResponseDto> findAll(Long taskId, String userEmail);
}
