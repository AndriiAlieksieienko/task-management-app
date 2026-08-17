package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.project.ProjectMemberCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberResponseDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.ProjectMapper;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceImplTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    private static final String CURRENT_USER_EMAIL = "manager@example.com";

    private User currentUser;
    private User teamMember;

    private Project project;

    private ProjectMemberCreateRequestDto createDto;
    private ProjectMemberResponseDto memberResponseDto;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail(CURRENT_USER_EMAIL);

        teamMember = new User();
        teamMember.setId(2L);
        teamMember.setUsername("teamMember");
        teamMember.setFirstName("Team");
        teamMember.setLastName("Member");

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");

        createDto = new ProjectMemberCreateRequestDto(teamMember.getId());

        memberResponseDto = new ProjectMemberResponseDto(
                teamMember.getId(),
                teamMember.getUsername(),
                teamMember.getFirstName(),
                teamMember.getLastName(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Add member - valid request - returns project member response")
    void addMember_ValidRequest_ReturnProjectMemberResponseDto() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByIdAndRoleName(teamMember.getId(), RoleName.ROLE_TEAM_MEMBER))
                .thenReturn(Optional.of(teamMember));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teamMember.getId()))
                .thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(projectMapper.toProjectMemberResponse(any(ProjectMember.class)))
                .thenReturn(memberResponseDto);

        ProjectMemberResponseDto actual =
                projectMemberService.addMember(project.getId(), createDto, CURRENT_USER_EMAIL);

        assertEquals(memberResponseDto, actual);

        verify(projectAccessService, times(1)).checkCanModifyProject(currentUser, project);

        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository, times(1)).save(captor.capture());

        ProjectMember savedMember = captor.getValue();
        assertEquals(project.getId(), savedMember.getId().getProjectId());
        assertEquals(teamMember.getId(), savedMember.getId().getUserId());
        assertEquals(project, savedMember.getProject());
        assertEquals(teamMember, savedMember.getUser());
    }

    @Test
    @DisplayName("Add member - current user not found - throws exception")
    void addMember_CurrentUserNotFound_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectMemberService.addMember(project.getId(), createDto, CURRENT_USER_EMAIL)
        );

        assertEquals("User not found by email: " + CURRENT_USER_EMAIL, exception.getMessage());

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add member - project not found - throws exception")
    void addMember_ProjectNotFound_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectMemberService.addMember(project.getId(), createDto, CURRENT_USER_EMAIL)
        );

        assertEquals("Can't find the project by id: " + project.getId(), exception.getMessage());

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add member - access denied - throws exception")
    void addMember_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(currentUser, project);

        assertThrows(
                AccessDeniedException.class,
                () -> projectMemberService.addMember(project.getId(), createDto, CURRENT_USER_EMAIL)
        );

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add member - user is not a team member - throws exception")
    void addMember_UserNotTeamMember_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByIdAndRoleName(teamMember.getId(), RoleName.ROLE_TEAM_MEMBER))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectMemberService.addMember(project.getId(), createDto, CURRENT_USER_EMAIL)
        );

        assertEquals(
                "Team member not found by id: " + teamMember.getId(),
                exception.getMessage()
        );

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add member - user already a member - throws exception")
    void addMember_UserAlreadyMember_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByIdAndRoleName(teamMember.getId(), RoleName.ROLE_TEAM_MEMBER))
                .thenReturn(Optional.of(teamMember));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teamMember.getId()))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> projectMemberService.addMember(project.getId(), createDto, CURRENT_USER_EMAIL)
        );

        assertEquals("User is already a member of this project", exception.getMessage());

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remove member - valid request - deletes member")
    void removeMember_ValidRequest_DeletesMember() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teamMember.getId()))
                .thenReturn(true);

        projectMemberService.removeMember(project.getId(), teamMember.getId(), CURRENT_USER_EMAIL);

        verify(projectAccessService, times(1)).checkCanModifyProject(currentUser, project);
        verify(projectMemberRepository, times(1))
                .deleteByIdProjectIdAndIdUserId(project.getId(), teamMember.getId());
    }

    @Test
    @DisplayName("Remove member - current user not found - throws exception")
    void removeMember_CurrentUserNotFound_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectMemberService.removeMember(
                        project.getId(), teamMember.getId(), CURRENT_USER_EMAIL
                )
        );

        verify(projectMemberRepository, never()).deleteByIdProjectIdAndIdUserId(any(), any());
    }

    @Test
    @DisplayName("Remove member - project not found - throws exception")
    void removeMember_ProjectNotFound_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectMemberService.removeMember(
                        project.getId(), teamMember.getId(), CURRENT_USER_EMAIL
                )
        );

        verify(projectMemberRepository, never()).deleteByIdProjectIdAndIdUserId(any(), any());
    }

    @Test
    @DisplayName("Remove member - access denied - throws exception")
    void removeMember_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        doThrow(new AccessDeniedException("Access denied"))
                .when(projectAccessService).checkCanModifyProject(currentUser, project);

        assertThrows(
                AccessDeniedException.class,
                () -> projectMemberService.removeMember(
                        project.getId(), teamMember.getId(), CURRENT_USER_EMAIL
                )
        );

        verify(projectMemberRepository, never()).deleteByIdProjectIdAndIdUserId(any(), any());
    }

    @Test
    @DisplayName("Remove member - user is not a member - throws exception")
    void removeMember_UserNotMember_ThrowsException() {
        when(userRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teamMember.getId()))
                .thenReturn(false);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectMemberService.removeMember(
                        project.getId(), teamMember.getId(), CURRENT_USER_EMAIL
                )
        );

        assertEquals("User is not a member of this project", exception.getMessage());

        verify(projectMemberRepository, never()).deleteByIdProjectIdAndIdUserId(any(), any());
    }
}
