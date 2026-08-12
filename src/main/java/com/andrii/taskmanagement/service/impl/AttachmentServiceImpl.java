package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.attachment.AttachmentCreateRequestDto;
import com.andrii.taskmanagement.dto.attachment.AttachmentResponseDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.AttachmentMapper;
import com.andrii.taskmanagement.model.Attachment;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.attachment.AttachmentRepository;
import com.andrii.taskmanagement.repository.task.TaskRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.AttachmentService;
import com.andrii.taskmanagement.service.ProjectAccessService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    @Override
    @Transactional
    public AttachmentResponseDto save(
            AttachmentCreateRequestDto requestDto,
            String userEmail
    ) {
        User user = findUserByEmail(userEmail);
        Task task = findTaskById(requestDto.taskId());

        projectAccessService.checkCanModifyProject(user, task.getProject());

        Attachment attachment = attachmentMapper.toModel(requestDto);

        attachment.setTask(task);
        attachment.setUploadedBy(user);
        attachment.setUploadDate(LocalDateTime.now());

        Attachment savedAttachment = attachmentRepository.save(attachment);

        return attachmentMapper.toDto(savedAttachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> findAll(Long taskId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Task task = findTaskById(taskId);

        projectAccessService.checkCanViewProject(user, task.getProject());

        return attachmentRepository.findAllByTaskId(taskId)
                .stream()
                .map(attachmentMapper::toDto)
                .toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found: " + email
                        )
                );
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Task not found: " + id
                        )
                );
    }
}
