package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.dto.task.TaskAssignedUpdateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskCreateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskResponseDto;
import com.andrii.taskmanagement.dto.task.TaskUpdateRequestDto;
import com.andrii.taskmanagement.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Task management", description = "Endpoints for managing tasks")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {
    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Create a task")
    public TaskResponseDto createTask(
            @Valid @RequestBody TaskCreateRequestDto requestDto,
            Authentication authentication
    ) {
        return taskService.save(requestDto, authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER')")
    @Operation(summary = "Get task by id")
    public TaskResponseDto getTaskById(@PathVariable Long id, Authentication authentication) {
        return taskService.findById(id, authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER')")
    @Operation(summary = "Get tasks for a project")
    public Page<TaskResponseDto> getTasks(
            @RequestParam Long projectId,
            Pageable pageable,
            Authentication authentication
    ) {
        return taskService.findAll(projectId, pageable, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Update a task")
    public TaskResponseDto updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequestDto requestDto,
            Authentication authentication
    ) {
        return taskService.updateById(id, requestDto, authentication.getName());
    }

    @PutMapping("/{id}/assigned")
    @PreAuthorize("hasRole('TEAM_MEMBER')")
    @Operation(summary = "Update assigned task")
    public TaskResponseDto updateAssignedTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskAssignedUpdateRequestDto requestDto,
            Authentication authentication
    ) {
        return taskService.updateAssignedById(id, requestDto, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            Authentication authentication
    ) {
        taskService.deleteById(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
