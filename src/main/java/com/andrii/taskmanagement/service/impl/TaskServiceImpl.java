package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.task.TaskAssignedUpdateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskCreateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskResponseDto;
import com.andrii.taskmanagement.dto.task.TaskUpdateRequestDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.TaskMapper;
import com.andrii.taskmanagement.model.Label;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.label.LabelRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.task.TaskRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import com.andrii.taskmanagement.service.TaskService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LabelRepository labelRepository;
    private final TaskMapper taskMapper;
    private final ProjectAccessService projectAccessService;

    @Override
    public TaskResponseDto save(TaskCreateRequestDto requestDto, String userEmail) {
        User user = findUserByEmail(userEmail);

        Project project = findProjectById(requestDto.projectId());

        projectAccessService.checkCanModifyProject(user, project);

        Task task = taskMapper.toModel(requestDto);
        task.setProject(project);
        task.setCreatedBy(user);
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        if (requestDto.assigneeId() != null) {
            User assignee = userRepository.findById(requestDto.assigneeId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Assignee not found: " + requestDto.assigneeId()
                    ));

            task.setAssignee(assignee);
        }

        if (requestDto.labelIds() != null) {
            task.setLabels(findLabelsByIds(requestDto.labelIds()));
        }

        Task savedTask = taskRepository.save(task);
        return taskMapper.toTaskResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDto findById(Long id, String userEmail) {
        Task task = findTaskById(id);
        User user = findUserByEmail(userEmail);

        projectAccessService.checkCanViewProject(user, task.getProject());

        return taskMapper.toTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponseDto> findAll(Long projectId, Pageable pageable, String userEmail) {
        Project project = findProjectById(projectId);
        User user = findUserByEmail(userEmail);

        projectAccessService.checkCanViewProject(user, project);

        return taskRepository.findAllByProjectId(projectId, pageable)
                .map(taskMapper::toTaskResponse);
    }

    @Override
    public void deleteById(Long id, String userEmail) {
        Task task = findTaskById(id);
        User user = findUserByEmail(userEmail);

        projectAccessService.checkCanModifyProject(user, task.getProject());
        taskRepository.delete(task);
    }

    @Override
    public TaskResponseDto updateById(Long id, TaskUpdateRequestDto requestDto, String userEmail) {
        Task task = findTaskById(id);
        User user = findUserByEmail(userEmail);

        projectAccessService.checkCanModifyProject(user, task.getProject());

        taskMapper.updateTaskFromDto(requestDto, task);

        if (requestDto.assigneeId() != null) {
            User assignee = userRepository.findById(requestDto.assigneeId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Assignee not found: " + requestDto.assigneeId()
                    ));

            task.setAssignee(assignee);
        }

        if (requestDto.labelIds() != null) {
            task.setLabels(findLabelsByIds(requestDto.labelIds()));
        }

        task.setUpdatedAt(LocalDateTime.now());

        Task updatedTask = taskRepository.save(task);
        return taskMapper.toTaskResponse(updatedTask);
    }

    @Override
    public TaskResponseDto updateAssignedById(
            Long id,
            TaskAssignedUpdateRequestDto requestDto,
            String userEmail
    ) {
        Task task = findTaskById(id);
        User user = findUserByEmail(userEmail);

        if (task.getAssignee() == null
                || !task.getAssignee().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You can only update tasks assigned to you"
            );
        }

        taskMapper.updateTaskFromAssignedDto(requestDto, task);
        task.setUpdatedAt(LocalDateTime.now());

        Task updatedTask = taskRepository.save(task);
        return taskMapper.toTaskResponse(updatedTask);
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find the task by id: " + id
                ));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found by email: " + email
                ));
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find the project by id: " + id
                ));
    }

    private Set<Label> findLabelsByIds(Set<Long> labelIds) {
        Set<Label> labels = new HashSet<>(
                labelRepository.findAllById(labelIds)
        );

        if (labels.size() != labelIds.size()) {
            throw new EntityNotFoundException("One or more labels not found");
        }

        return labels;
    }
}
