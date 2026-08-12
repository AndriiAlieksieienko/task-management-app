package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.dto.attachment.AttachmentCreateRequestDto;
import com.andrii.taskmanagement.dto.attachment.AttachmentResponseDto;
import com.andrii.taskmanagement.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attachment management", description = "Endpoints for managing attachments")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/attachments")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {
    private final AttachmentService attachmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER')")
    @Operation(summary = "Create an attachment")
    public AttachmentResponseDto createAttachment(
            @Valid @RequestBody AttachmentCreateRequestDto requestDto,
            Authentication authentication
    ) {
        return attachmentService.save(requestDto, authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER')")
    @Operation(summary = "Get attachments for a task")
    public List<AttachmentResponseDto> getAttachments(
            @RequestParam Long taskId,
            Authentication authentication
    ) {
        return attachmentService.findAll(taskId, authentication.getName());
    }
}
