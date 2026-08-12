package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.comment.CommentCreateRequestDto;
import com.andrii.taskmanagement.dto.comment.CommentResponseDto;
import java.util.List;

public interface CommentService {
    CommentResponseDto save(
            CommentCreateRequestDto requestDto,
            String userEmail
    );

    List<CommentResponseDto> findAll(
            Long taskId,
            String userEmail
    );
}
