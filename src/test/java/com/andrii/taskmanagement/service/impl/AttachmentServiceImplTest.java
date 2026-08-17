package com.andrii.taskmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.taskmanagement.dto.attachment.AttachmentCreateRequestDto;
import com.andrii.taskmanagement.dto.attachment.AttachmentResponseDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.AttachmentMapper;
import com.andrii.taskmanagement.model.Attachment;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.attachment.AttachmentRepository;
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
class AttachmentServiceImplTest {
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AttachmentMapper attachmentMapper;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private static final String USER_EMAIL = "manager@example.com";

    private User user;
    private Project project;
    private Task task;
    private Attachment attachment;

    private AttachmentCreateRequestDto createDto;
    private AttachmentResponseDto attachmentResponseDto;

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

        createDto = new AttachmentCreateRequestDto(
                task.getId(),
                "dropbox-file-id-123",
                "design.pdf"
        );

        attachment = new Attachment();
        attachment.setId(1000L);
        attachment.setTask(task);
        attachment.setUploadedBy(user);
        attachment.setDropboxFileId(createDto.dropboxFileId());
        attachment.setFilename(createDto.filename());
        attachment.setUploadDate(LocalDateTime.now());

        attachmentResponseDto = new AttachmentResponseDto(
                attachment.getId(),
                task.getId(),
                user.getId(),
                attachment.getDropboxFileId(),
                attachment.getFilename(),
                attachment.getUploadDate()
        );
    }

    @Test
    @DisplayName("Save attachment - valid request - returns attachment response")
    void save_ValidRequest_ReturnAttachmentResponseDto() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(attachmentMapper.toModel(createDto)).thenReturn(attachment);
        when(attachmentRepository.save(attachment)).thenReturn(attachment);
        when(attachmentMapper.toDto(attachment)).thenReturn(attachmentResponseDto);

        AttachmentResponseDto actual = attachmentService.save(createDto, USER_EMAIL);

        assertEquals(attachmentResponseDto, actual);
        assertEquals(task, attachment.getTask());
        assertEquals(user, attachment.getUploadedBy());

        verify(projectAccessService, times(1)).checkCanModifyProject(user, project);
        verify(attachmentRepository, times(1)).save(attachment);
    }

    @Test
    @DisplayName("Save attachment - user not found - throws exception")
    void save_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> attachmentService.save(createDto, USER_EMAIL)
        );

        assertEquals("User not found: " + USER_EMAIL, exception.getMessage());

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save attachment - task not found - throws exception")
    void save_TaskNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> attachmentService.save(createDto, USER_EMAIL)
        );

        assertEquals("Task not found: " + task.getId(), exception.getMessage());

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save attachment - access denied - throws exception")
    void save_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> attachmentService.save(createDto, USER_EMAIL)
        );

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Find all attachments - authorized user - returns attachments")
    void findAll_AuthorizedUser_ReturnsAttachments() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(attachmentRepository.findAllByTaskId(task.getId())).thenReturn(List.of(attachment));
        when(attachmentMapper.toDto(attachment)).thenReturn(attachmentResponseDto);

        List<AttachmentResponseDto> actual = attachmentService.findAll(task.getId(), USER_EMAIL);

        assertEquals(1, actual.size());
        assertEquals(attachmentResponseDto, actual.get(0));

        verify(projectAccessService, times(1)).checkCanViewProject(user, project);
    }

    @Test
    @DisplayName("Find all attachments - no attachments - returns empty list")
    void findAll_NoAttachments_ReturnsEmptyList() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(attachmentRepository.findAllByTaskId(task.getId())).thenReturn(List.of());

        List<AttachmentResponseDto> actual = attachmentService.findAll(task.getId(), USER_EMAIL);

        assertEquals(0, actual.size());
    }

    @Test
    @DisplayName("Find all attachments - user not found - throws exception")
    void findAll_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> attachmentService.findAll(task.getId(), USER_EMAIL)
        );

        verify(attachmentRepository, never()).findAllByTaskId(any());
    }

    @Test
    @DisplayName("Find all attachments - task not found - throws exception")
    void findAll_TaskNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> attachmentService.findAll(task.getId(), USER_EMAIL)
        );

        assertEquals("Task not found: " + task.getId(), exception.getMessage());

        verify(attachmentRepository, never()).findAllByTaskId(any());
    }

    @Test
    @DisplayName("Find all attachments - access denied - throws exception")
    void findAll_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanViewProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> attachmentService.findAll(task.getId(), USER_EMAIL)
        );

        verify(attachmentRepository, never()).findAllByTaskId(any());
    }
}
