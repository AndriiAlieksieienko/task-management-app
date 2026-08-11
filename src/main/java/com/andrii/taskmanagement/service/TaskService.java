package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.task.TaskAssignedUpdateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskCreateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskResponseDto;
import com.andrii.taskmanagement.dto.task.TaskUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    TaskResponseDto save(TaskCreateRequestDto requestDto, String userEmail);

    TaskResponseDto findById(Long id, String userEmail);

    Page<TaskResponseDto> findAll(Long projectId, Pageable pageable, String userEmail);

    void deleteById(Long id, String userEmail);

    TaskResponseDto updateById(Long id, TaskUpdateRequestDto requestDto, String userEmail);

    TaskResponseDto updateAssignedById(
            Long id,
            TaskAssignedUpdateRequestDto requestDto,
            String userEmail
    );
}
