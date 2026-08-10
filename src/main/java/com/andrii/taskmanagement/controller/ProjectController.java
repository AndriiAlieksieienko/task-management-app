package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectManagerUpdateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectSearchParameters;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import com.andrii.taskmanagement.service.ProjectMemberService;
import com.andrii.taskmanagement.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project management", description = "Endpoints for managing projects")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new project")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectResponseDto createProject(
            Authentication authentication,
            @RequestBody @Valid ProjectCreateRequestDto requestDto
    ) {
        return projectService.save(requestDto, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Get projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER')")
    public Page<ProjectResponseDto> getAll(
            ProjectSearchParameters searchParameters,
            Pageable pageable,
            Authentication authentication
    ) {
        return projectService.findAll(searchParameters, pageable, authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER')")
    public ProjectResponseDto getProjectById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return projectService.findById(id, authentication.getName());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update the project")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectResponseDto updateProject(
            @PathVariable Long id,
            @RequestBody @Valid ProjectUpdateRequestDto requestDto,
            Authentication authentication
    ) {
        return projectService.updateById(id, requestDto, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete the project")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        projectService.deleteById(id, authentication.getName());
    }

    @PutMapping("/{id}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change project manager")
    public ProjectResponseDto updateProjectManager(
            @PathVariable Long id,
            @RequestBody @Valid ProjectManagerUpdateRequestDto requestDto
    ) {
        return projectService.updateProjectManager(id, requestDto);
    }

    @PostMapping("/{projectId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Add a team member to the project")
    public ProjectMemberResponseDto addMember(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectMemberCreateRequestDto requestDto,
            Authentication authentication
    ) {
        return projectMemberService.addMember(projectId, requestDto, authentication.getName());
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a team member from the project")
    public void removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        projectMemberService.removeMember(projectId, userId, authentication.getName());
    }
}
