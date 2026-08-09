package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectManagerUpdateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectSearchParameters;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    ProjectResponseDto save(ProjectCreateRequestDto requestDto, String ownerEmail);

    ProjectResponseDto findById(Long id, String ownerEmail);

    Page<ProjectResponseDto> findAll(
            ProjectSearchParameters searchParameters,
            Pageable pageable,
            String ownerEmail
    );

    void deleteById(Long id, String ownerEmail);

    ProjectResponseDto updateById(Long id, ProjectUpdateRequestDto requestDto, String ownerEmail);

    ProjectResponseDto updateProjectManager(Long id, ProjectManagerUpdateRequestDto requestDto);
}
