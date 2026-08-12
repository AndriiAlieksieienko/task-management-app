package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.label.LabelCreateRequestDto;
import com.andrii.taskmanagement.dto.label.LabelResponseDto;
import com.andrii.taskmanagement.dto.label.LabelUpdateRequestDto;
import java.util.List;

public interface LabelService {
    LabelResponseDto save(LabelCreateRequestDto requestDto, String userEmail);

    List<LabelResponseDto> findAll(Long projectId, String userEmail);

    LabelResponseDto updateById(
            Long id,
            LabelUpdateRequestDto requestDto,
            String userEmail
    );

    void deleteById(Long id, String userEmail);
}
