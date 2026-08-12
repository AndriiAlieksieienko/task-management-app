package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.comment.CommentCreateRequestDto;
import com.andrii.taskmanagement.dto.comment.CommentResponseDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.CommentMapper;
import com.andrii.taskmanagement.model.Comment;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.comment.CommentRepository;
import com.andrii.taskmanagement.repository.task.TaskRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.CommentService;
import com.andrii.taskmanagement.service.ProjectAccessService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final ProjectAccessService projectAccessService;

    @Override
    public CommentResponseDto save(
            CommentCreateRequestDto requestDto,
            String userEmail
    ) {
        User user = findUserByEmail(userEmail);
        Task task = findTaskById(requestDto.taskId());

        projectAccessService.checkCanViewProject(user, task.getProject());

        Comment comment = commentMapper.toModel(requestDto);

        comment.setTask(task);
        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> findAll(Long taskId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Task task = findTaskById(taskId);

        projectAccessService.checkCanViewProject(user, task.getProject());

        return commentRepository.findAllByTaskId(taskId)
                .stream()
                .map(commentMapper::toCommentResponse)
                .toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found by email: " + email
                ));
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find the task by id: " + id
                ));
    }
}
