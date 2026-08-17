package com.andrii.taskmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.taskmanagement.dto.comment.CommentCreateRequestDto;
import com.andrii.taskmanagement.dto.comment.CommentResponseDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.CommentMapper;
import com.andrii.taskmanagement.model.Comment;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.comment.CommentRepository;
import com.andrii.taskmanagement.repository.task.TaskRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private static final String USER_EMAIL = "member@example.com";

    private User user;
    private Project project;
    private Task task;
    private Comment comment;

    private CommentCreateRequestDto createDto;
    private CommentResponseDto commentResponseDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail(USER_EMAIL);

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");

        task = new Task();
        task.setId(100L);
        task.setProject(project);
        task.setName("Test Task");

        createDto = new CommentCreateRequestDto(task.getId(), "This looks good to me");

        comment = new Comment();
        comment.setId(1000L);
        comment.setTask(task);
        comment.setUser(user);
        comment.setText(createDto.text());
        comment.setCreatedAt(LocalDateTime.now());

        commentResponseDto = new CommentResponseDto(
                comment.getId(),
                task.getId(),
                user.getId(),
                comment.getText(),
                comment.getCreatedAt()
        );
    }

    @Test
    @DisplayName("Save comment - valid request - returns comment response")
    void save_ValidRequest_ReturnCommentResponseDto() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(commentMapper.toModel(createDto)).thenReturn(comment);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toCommentResponse(comment)).thenReturn(commentResponseDto);

        CommentResponseDto actual = commentService.save(createDto, USER_EMAIL);

        assertEquals(commentResponseDto, actual);
        assertEquals(task, comment.getTask());
        assertEquals(user, comment.getUser());

        verify(projectAccessService, times(1)).checkCanViewProject(user, project);
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    @DisplayName("Save comment - user not found - throws exception")
    void save_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> commentService.save(createDto, USER_EMAIL)
        );

        assertEquals("User not found by email: " + USER_EMAIL, exception.getMessage());

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save comment - task not found - throws exception")
    void save_TaskNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> commentService.save(createDto, USER_EMAIL)
        );

        assertEquals("Can't find the task by id: " + task.getId(), exception.getMessage());

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save comment - access denied - throws exception")
    void save_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanViewProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> commentService.save(createDto, USER_EMAIL)
        );

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Find all comments - authorized user - returns comments")
    void findAll_AuthorizedUser_ReturnsComments() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(commentRepository.findAllByTaskId(task.getId())).thenReturn(List.of(comment));
        when(commentMapper.toCommentResponse(comment)).thenReturn(commentResponseDto);

        List<CommentResponseDto> actual = commentService.findAll(task.getId(), USER_EMAIL);

        assertEquals(1, actual.size());
        assertEquals(commentResponseDto, actual.get(0));

        verify(projectAccessService, times(1)).checkCanViewProject(user, project);
    }

    @Test
    @DisplayName("Find all comments - no comments - returns empty list")
    void findAll_NoComments_ReturnsEmptyList() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(commentRepository.findAllByTaskId(task.getId())).thenReturn(List.of());

        List<CommentResponseDto> actual = commentService.findAll(task.getId(), USER_EMAIL);

        assertEquals(0, actual.size());
    }

    @Test
    @DisplayName("Find all comments - user not found - throws exception")
    void findAll_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> commentService.findAll(task.getId(), USER_EMAIL)
        );

        verify(commentRepository, never()).findAllByTaskId(any());
    }

    @Test
    @DisplayName("Find all comments - task not found - throws exception")
    void findAll_TaskNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> commentService.findAll(task.getId(), USER_EMAIL)
        );

        assertEquals("Can't find the task by id: " + task.getId(), exception.getMessage());

        verify(commentRepository, never()).findAllByTaskId(any());
    }

    @Test
    @DisplayName("Find all comments - access denied - throws exception")
    void findAll_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanViewProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> commentService.findAll(task.getId(), USER_EMAIL)
        );

        verify(commentRepository, never()).findAllByTaskId(any());
    }
}
