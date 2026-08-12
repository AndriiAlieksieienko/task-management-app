package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.dto.label.LabelCreateRequestDto;
import com.andrii.taskmanagement.dto.label.LabelResponseDto;
import com.andrii.taskmanagement.dto.label.LabelUpdateRequestDto;
import com.andrii.taskmanagement.service.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Label management", description = "Endpoints for managing labels")
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new label")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public LabelResponseDto createLabel(
            @RequestBody @Valid LabelCreateRequestDto requestDto,
            Authentication authentication
    ) {
        return labelService.save(requestDto, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Get labels by project")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER')")
    public List<LabelResponseDto> getAll(
            @RequestParam Long projectId,
            Authentication authentication
    ) {
        return labelService.findAll(projectId, authentication.getName());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update the label")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public LabelResponseDto updateLabel(
            @PathVariable Long id,
            @RequestBody @Valid LabelUpdateRequestDto requestDto,
            Authentication authentication
    ) {
        return labelService.updateById(id, requestDto, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete the label")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        labelService.deleteById(id, authentication.getName());
    }
}
