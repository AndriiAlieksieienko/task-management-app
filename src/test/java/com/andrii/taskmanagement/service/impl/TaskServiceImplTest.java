package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.task.TaskAssignedUpdateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskCreateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskResponseDto;
import com.andrii.taskmanagement.dto.task.TaskUpdateRequestDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.TaskMapper;
import com.andrii.taskmanagement.model.Label;
import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.TaskStatus;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.label.LabelRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.task.TaskRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private static final String CREATOR_EMAIL = "manager@example.com";
    private static final String ASSIGNEE_EMAIL = "assignee@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    private User creator;
    private User assignee;
    private User otherUser;

    private Project project;

    private Task task;
    private TaskResponseDto taskResponseDto;

    private TaskCreateRequestDto createDto;
    private TaskUpdateRequestDto updateDto;
    private TaskAssignedUpdateRequestDto assignedUpdateDto;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setEmail(CREATOR_EMAIL);

        assignee = new User();
        assignee.setId(2L);
        assignee.setEmail(ASSIGNEE_EMAIL);

        otherUser = new User();
        otherUser.setId(3L);
        otherUser.setEmail(OTHER_EMAIL);

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");

        createDto = new TaskCreateRequestDto(
                project.getId(),
                assignee.getId(),
                "Implement login page",
                "Build the login screen",
                Priority.HIGH,
                TaskStatus.NOT_STARTED,
                LocalDate.of(2026, 9, 1),
                Set.of(1L, 2L)
        );

        updateDto = new TaskUpdateRequestDto(
                assignee.getId(),
                "Updated name",
                "Updated description",
                Priority.MEDIUM,
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 10, 1),
                Set.of(1L)
        );

        assignedUpdateDto = new TaskAssignedUpdateRequestDto(
                "Updated by assignee",
                TaskStatus.COMPLETED,
                LocalDate.of(2026, 11, 1)
        );

        task = new Task();
        task.setId(100L);
        task.setProject(project);
        task.setCreatedBy(creator);
        task.setAssignee(assignee);
        task.setName("Implement login page");
        task.setDescription("Build the login screen");
        task.setPriority(Priority.HIGH);
        task.setStatus(TaskStatus.NOT_STARTED);
        task.setDueDate(LocalDate.of(2026, 9, 1));

        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        taskResponseDto = new TaskResponseDto();
        taskResponseDto.setId(task.getId());
        taskResponseDto.setProjectId(project.getId());
        taskResponseDto.setAssigneeId(assignee.getId());
        taskResponseDto.setCreatedById(creator.getId());
        taskResponseDto.setName(task.getName());
        taskResponseDto.setDescription(task.getDescription());
        taskResponseDto.setPriority(task.getPriority());
        taskResponseDto.setStatus(task.getStatus());
        taskResponseDto.setDueDate(task.getDueDate());
        taskResponseDto.setCreatedAt(task.getCreatedAt());
        taskResponseDto.setUpdatedAt(task.getUpdatedAt());
        taskResponseDto.setLabelIds(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("Save task - valid request with assignee and labels - returns task")
    void save_ValidRequestWithAssigneeAndLabels_ReturnTaskResponseDto() {
        Label labelOne = new Label();
        labelOne.setId(1L);

        Label labelTwo = new Label();
        labelTwo.setId(2L);

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskMapper.toModel(createDto)).thenReturn(task);
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(labelRepository.findAllById(createDto.labelIds()))
                .thenReturn(List.of(labelOne, labelTwo));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponseDto);

        TaskResponseDto actual = taskService.save(createDto, CREATOR_EMAIL);

        assertEquals(taskResponseDto, actual);
        assertEquals(project, task.getProject());
        assertEquals(creator, task.getCreatedBy());
        assertEquals(assignee, task.getAssignee());

        verify(projectAccessService, times(1)).checkCanModifyProject(creator, project);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    @DisplayName("Save task - no assignee and no labels - returns task")
    void save_NoAssigneeNoLabels_ReturnTaskResponseDto() {
        TaskCreateRequestDto dtoWithoutExtras = new TaskCreateRequestDto(
                project.getId(),
                null,
                "Simple task",
                null,
                Priority.LOW,
                TaskStatus.NOT_STARTED,
                null,
                null
        );

        Task plainTask = new Task();
        plainTask.setId(101L);
        plainTask.setName("Simple task");
        plainTask.setStatus(TaskStatus.NOT_STARTED);
        plainTask.setPriority(Priority.LOW);

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskMapper.toModel(dtoWithoutExtras)).thenReturn(plainTask);
        when(taskRepository.save(plainTask)).thenReturn(plainTask);
        when(taskMapper.toTaskResponse(plainTask)).thenReturn(taskResponseDto);

        taskService.save(dtoWithoutExtras, CREATOR_EMAIL);

        verify(userRepository, never()).findById(any());
        verify(labelRepository, never()).findAllById(any());
        verify(taskRepository, times(1)).save(plainTask);
    }

    @Test
    @DisplayName("Save task - creator not found - throws exception")
    void save_CreatorNotFound_ThrowsException() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.save(createDto, CREATOR_EMAIL)
        );

        assertEquals("User not found by email: " + CREATOR_EMAIL, exception.getMessage());

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save task - project not found - throws exception")
    void save_ProjectNotFound_ThrowsException() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> taskService.save(createDto, CREATOR_EMAIL)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save task - access denied - throws exception")
    void save_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(creator, project);

        assertThrows(
                AccessDeniedException.class,
                () -> taskService.save(createDto, CREATOR_EMAIL)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save task - assignee not found - throws exception")
    void save_AssigneeNotFound_ThrowsException() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskMapper.toModel(createDto)).thenReturn(task);
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.save(createDto, CREATOR_EMAIL)
        );

        assertEquals("Assignee not found: " + assignee.getId(), exception.getMessage());

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save task - one or more labels not found - throws exception")
    void save_LabelNotFound_ThrowsException() {
        Label labelOne = new Label();
        labelOne.setId(1L);

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskMapper.toModel(createDto)).thenReturn(task);
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(labelRepository.findAllById(createDto.labelIds())).thenReturn(List.of(labelOne));

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.save(createDto, CREATOR_EMAIL)
        );

        assertEquals("One or more labels not found", exception.getMessage());

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Find task by id - authorized user - returns task")
    void findById_AuthorizedUser_ReturnTaskResponseDto() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(ASSIGNEE_EMAIL)).thenReturn(Optional.of(assignee));
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponseDto);

        TaskResponseDto actual = taskService.findById(task.getId(), ASSIGNEE_EMAIL);

        assertEquals(taskResponseDto, actual);

        verify(projectAccessService, times(1)).checkCanViewProject(assignee, project);
    }

    @Test
    @DisplayName("Find task by id - nonexistent id - throws exception")
    void findById_NonexistentId_ThrowsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.findById(999L, ASSIGNEE_EMAIL)
        );

        assertEquals("Can't find the task by id: 999", exception.getMessage());
    }

    @Test
    @DisplayName("Find task by id - access denied - throws exception")
    void findById_AccessDenied_ThrowsException() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(otherUser));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanViewProject(otherUser, project);

        assertThrows(
                AccessDeniedException.class,
                () -> taskService.findById(task.getId(), OTHER_EMAIL)
        );
    }

    @Test
    @DisplayName("Find all tasks - authorized user - returns page")
    void findAll_AuthorizedUser_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task));

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(ASSIGNEE_EMAIL)).thenReturn(Optional.of(assignee));
        when(taskRepository.findAllByProjectId(project.getId(), pageable)).thenReturn(taskPage);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponseDto);

        Page<TaskResponseDto> actual =
                taskService.findAll(project.getId(), pageable, ASSIGNEE_EMAIL);

        assertEquals(1, actual.getTotalElements());
        assertEquals(taskResponseDto, actual.getContent().get(0));

        verify(projectAccessService, times(1)).checkCanViewProject(assignee, project);
    }

    @Test
    @DisplayName("Find all tasks - project not found - throws exception")
    void findAll_ProjectNotFound_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> taskService.findAll(999L, pageable, ASSIGNEE_EMAIL)
        );

        verify(taskRepository, never()).findAllByProjectId(any(), any());
    }

    @Test
    @DisplayName("Find all tasks - access denied - throws exception")
    void findAll_AccessDenied_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(otherUser));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanViewProject(otherUser, project);

        assertThrows(
                AccessDeniedException.class,
                () -> taskService.findAll(project.getId(), pageable, OTHER_EMAIL)
        );

        verify(taskRepository, never()).findAllByProjectId(any(), any());
    }

    @Test
    @DisplayName("Delete task - authorized user - deletes task")
    void deleteById_AuthorizedUser_DeletesTask() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));

        taskService.deleteById(task.getId(), CREATOR_EMAIL);

        verify(projectAccessService, times(1)).checkCanModifyProject(creator, project);
        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    @DisplayName("Delete task - nonexistent id - throws exception")
    void deleteById_NonexistentId_ThrowsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> taskService.deleteById(999L, CREATOR_EMAIL)
        );

        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete task - access denied - throws exception")
    void deleteById_AccessDenied_ThrowsException() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(otherUser));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(otherUser, project);

        assertThrows(
                AccessDeniedException.class,
                () -> taskService.deleteById(task.getId(), OTHER_EMAIL)
        );

        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Update task - valid request - returns updated task")
    void updateById_ValidRequest_ReturnUpdatedTaskResponseDto() {
        Label labelOne = new Label();
        labelOne.setId(1L);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(labelRepository.findAllById(updateDto.labelIds())).thenReturn(List.of(labelOne));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponseDto);

        TaskResponseDto actual = taskService.updateById(task.getId(), updateDto, CREATOR_EMAIL);

        assertEquals(taskResponseDto, actual);

        verify(projectAccessService, times(1)).checkCanModifyProject(creator, project);
        verify(taskMapper, times(1)).updateTaskFromDto(updateDto, task);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    @DisplayName("Update task - nonexistent id - throws exception")
    void updateById_NonexistentId_ThrowsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateById(999L, updateDto, CREATOR_EMAIL)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update task - access denied - throws exception")
    void updateById_AccessDenied_ThrowsException() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(otherUser));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(otherUser, project);

        assertThrows(
                AccessDeniedException.class,
                () -> taskService.updateById(task.getId(), updateDto, OTHER_EMAIL)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update task - assignee not found - throws exception")
    void updateById_AssigneeNotFound_ThrowsException() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateById(task.getId(), updateDto, CREATOR_EMAIL)
        );

        assertEquals("Assignee not found: " + assignee.getId(), exception.getMessage());

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update task - one or more labels not found - throws exception")
    void updateById_LabelNotFound_ThrowsException() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(labelRepository.findAllById(updateDto.labelIds())).thenReturn(List.of());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateById(task.getId(), updateDto, CREATOR_EMAIL)
        );

        assertEquals("One or more labels not found", exception.getMessage());

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update assigned task - assignee - returns updated task")
    void updateAssignedById_Assignee_ReturnUpdatedTaskResponseDto() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(ASSIGNEE_EMAIL)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponseDto);

        TaskResponseDto actual =
                taskService.updateAssignedById(task.getId(), assignedUpdateDto, ASSIGNEE_EMAIL);

        assertEquals(taskResponseDto, actual);

        verify(taskMapper, times(1)).updateTaskFromAssignedDto(assignedUpdateDto, task);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    @DisplayName("Update assigned task - not the assignee - throws access denied")
    void updateAssignedById_NotAssignee_ThrowsAccessDeniedException() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(otherUser));

        assertThrows(
                AccessDeniedException.class,
                () -> taskService.updateAssignedById(task.getId(), assignedUpdateDto, OTHER_EMAIL)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update assigned task - task has no assignee - throws access denied")
    void updateAssignedById_NoAssignee_ThrowsAccessDeniedException() {
        task.setAssignee(null);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(ASSIGNEE_EMAIL)).thenReturn(Optional.of(assignee));

        assertThrows(
                AccessDeniedException.class,
                () -> taskService.updateAssignedById(task.getId(), assignedUpdateDto, ASSIGNEE_EMAIL)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update assigned task - nonexistent id - throws exception")
    void updateAssignedById_NonexistentId_ThrowsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateAssignedById(999L, assignedUpdateDto, ASSIGNEE_EMAIL)
        );

        verify(taskRepository, never()).save(any());
    }
}
