package com.andrii.taskmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.taskmanagement.dto.label.LabelCreateRequestDto;
import com.andrii.taskmanagement.dto.label.LabelResponseDto;
import com.andrii.taskmanagement.dto.label.LabelUpdateRequestDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.LabelMapper;
import com.andrii.taskmanagement.model.Label;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.label.LabelRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
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
class LabelServiceImplTest {
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LabelMapper labelMapper;
    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private LabelServiceImpl labelService;

    private static final String USER_EMAIL = "manager@example.com";

    private User user;
    private Project project;
    private Label label;

    private LabelCreateRequestDto createDto;
    private LabelUpdateRequestDto updateDto;
    private LabelResponseDto labelResponseDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail(USER_EMAIL);

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");

        createDto = new LabelCreateRequestDto(project.getId(), "Bug", "#FF0000");
        updateDto = new LabelUpdateRequestDto("New Name", "#FFFFFF");

        label = new Label();
        label.setId(100L);
        label.setProject(project);
        label.setName("Bug");
        label.setColor("#FF0000");
        label.setCreatedAt(LocalDateTime.now());

        labelResponseDto = new LabelResponseDto(
                label.getId(),
                project.getId(),
                label.getName(),
                label.getColor(),
                label.getCreatedAt()
        );
    }

    @Test
    @DisplayName("Save label - valid request - returns label response")
    void save_ValidRequest_ReturnLabelResponseDto() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(labelMapper.toModel(createDto)).thenReturn(label);
        when(labelRepository.save(label)).thenReturn(label);
        when(labelMapper.toDto(label)).thenReturn(labelResponseDto);

        LabelResponseDto actual = labelService.save(createDto, USER_EMAIL);

        assertEquals(labelResponseDto, actual);
        assertEquals(project, label.getProject());

        verify(projectAccessService, times(1)).checkCanModifyProject(user, project);
        verify(labelRepository, times(1)).save(label);
    }

    @Test
    @DisplayName("Save label - user not found - throws exception")
    void save_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.save(createDto, USER_EMAIL)
        );

        assertEquals("User not found with email: " + USER_EMAIL, exception.getMessage());

        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save label - project not found - throws exception")
    void save_ProjectNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.save(createDto, USER_EMAIL)
        );

        assertEquals("Project not found with id: " + project.getId(), exception.getMessage());

        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save label - access denied - throws exception")
    void save_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> labelService.save(createDto, USER_EMAIL)
        );

        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Find all labels - authorized user - returns labels")
    void findAll_AuthorizedUser_ReturnsLabels() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(labelRepository.findAllByProjectId(project.getId())).thenReturn(List.of(label));
        when(labelMapper.toDto(label)).thenReturn(labelResponseDto);

        List<LabelResponseDto> actual = labelService.findAll(project.getId(), USER_EMAIL);

        assertEquals(1, actual.size());
        assertEquals(labelResponseDto, actual.get(0));

        verify(projectAccessService, times(1)).checkCanViewProject(user, project);
    }

    @Test
    @DisplayName("Find all labels - no labels - returns empty list")
    void findAll_NoLabels_ReturnsEmptyList() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(labelRepository.findAllByProjectId(project.getId())).thenReturn(List.of());

        List<LabelResponseDto> actual = labelService.findAll(project.getId(), USER_EMAIL);

        assertEquals(0, actual.size());
    }

    @Test
    @DisplayName("Find all labels - user not found - throws exception")
    void findAll_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> labelService.findAll(project.getId(), USER_EMAIL)
        );

        verify(labelRepository, never()).findAllByProjectId(any());
    }

    @Test
    @DisplayName("Find all labels - project not found - throws exception")
    void findAll_ProjectNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.findAll(project.getId(), USER_EMAIL)
        );

        assertEquals("Project not found with id: " + project.getId(), exception.getMessage());

        verify(labelRepository, never()).findAllByProjectId(any());
    }

    @Test
    @DisplayName("Find all labels - access denied - throws exception")
    void findAll_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanViewProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> labelService.findAll(project.getId(), USER_EMAIL)
        );

        verify(labelRepository, never()).findAllByProjectId(any());
    }

    @Test
    @DisplayName("Update label - valid request - returns updated label")
    void updateById_ValidRequest_ReturnUpdatedLabelResponseDto() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(labelRepository.findById(label.getId())).thenReturn(Optional.of(label));
        when(labelRepository.save(label)).thenReturn(label);
        when(labelMapper.toDto(label)).thenReturn(labelResponseDto);

        LabelResponseDto actual = labelService.updateById(label.getId(), updateDto, USER_EMAIL);

        assertEquals(labelResponseDto, actual);

        verify(projectAccessService, times(1)).checkCanModifyProject(user, project);
        verify(labelMapper, times(1)).updateLabelFromDto(updateDto, label);
        verify(labelRepository, times(1)).save(label);
    }

    @Test
    @DisplayName("Update label - user not found - throws exception")
    void updateById_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> labelService.updateById(label.getId(), updateDto, USER_EMAIL)
        );

        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update label - nonexistent id - throws exception")
    void updateById_NonexistentId_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(labelRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.updateById(999L, updateDto, USER_EMAIL)
        );

        assertEquals("Label not found with id: 999", exception.getMessage());

        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update label - access denied - throws exception")
    void updateById_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(labelRepository.findById(label.getId())).thenReturn(Optional.of(label));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> labelService.updateById(label.getId(), updateDto, USER_EMAIL)
        );

        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete label - valid id - deletes label")
    void deleteById_ValidId_DeletesLabel() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(labelRepository.findById(label.getId())).thenReturn(Optional.of(label));

        labelService.deleteById(label.getId(), USER_EMAIL);

        verify(projectAccessService, times(1)).checkCanModifyProject(user, project);
        verify(labelRepository, times(1)).delete(label);
    }

    @Test
    @DisplayName("Delete label - user not found - throws exception")
    void deleteById_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> labelService.deleteById(label.getId(), USER_EMAIL)
        );

        verify(labelRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete label - nonexistent id - throws exception")
    void deleteById_NonexistentId_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(labelRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> labelService.deleteById(999L, USER_EMAIL)
        );

        assertEquals("Label not found with id: 999", exception.getMessage());

        verify(labelRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete label - access denied - throws exception")
    void deleteById_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(labelRepository.findById(label.getId())).thenReturn(Optional.of(label));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(user, project);

        assertThrows(
                AccessDeniedException.class,
                () -> labelService.deleteById(label.getId(), USER_EMAIL)
        );

        verify(labelRepository, never()).delete(any());
    }
}
