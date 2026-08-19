package com.andrii.taskmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.Role;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceImplTest {
    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAccessServiceImpl projectAccessService;

    private Role adminRole;
    private Role projectManagerRole;
    private Role teamMemberRole;

    private User admin;
    private User projectManager;
    private User otherProjectManager;
    private User teamMember;
    private User otherTeamMember;

    private Project project;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ROLE_ADMIN);

        projectManagerRole = new Role();
        projectManagerRole.setId(2L);
        projectManagerRole.setName(RoleName.ROLE_PROJECT_MANAGER);

        teamMemberRole = new Role();
        teamMemberRole.setId(3L);
        teamMemberRole.setName(RoleName.ROLE_TEAM_MEMBER);

        admin = new User();
        admin.setId(1L);
        admin.setRole(adminRole);

        projectManager = new User();
        projectManager.setId(2L);
        projectManager.setRole(projectManagerRole);

        otherProjectManager = new User();
        otherProjectManager.setId(3L);
        otherProjectManager.setRole(projectManagerRole);

        teamMember = new User();
        teamMember.setId(4L);
        teamMember.setRole(teamMemberRole);

        otherTeamMember = new User();
        otherTeamMember.setId(5L);
        otherTeamMember.setRole(teamMemberRole);

        project = new Project();
        project.setId(100L);
        project.setProjectManager(projectManager);
    }

    @Test
    @DisplayName("Can view project - admin - always allowed")
    void checkCanViewProject_Admin_DoesNotThrow() {
        assertDoesNotThrow(() -> projectAccessService.checkCanViewProject(admin, project));

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Can view project - project's own manager - allowed")
    void checkCanViewProject_ProjectsOwnManager_DoesNotThrow() {
        assertDoesNotThrow(
                () -> projectAccessService.checkCanViewProject(projectManager, project)
        );

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Can view project - a different project's manager - denied")
    void checkCanViewProject_DifferentProjectManager_ThrowsAccessDenied() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.checkCanViewProject(otherProjectManager, project)
        );

        assertEquals("You don't have access to this project", exception.getMessage());

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Can view project - team member who is a project member - allowed")
    void checkCanViewProject_TeamMemberIsProjectMember_DoesNotThrow() {
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teamMember.getId()))
                .thenReturn(true);

        assertDoesNotThrow(() -> projectAccessService.checkCanViewProject(teamMember, project));

        verify(projectMemberRepository, times(1))
                .existsByProjectIdAndUserId(project.getId(), teamMember.getId());
    }

    @Test
    @DisplayName("Can view project - team member who is not a project member - denied")
    void checkCanViewProject_TeamMemberNotProjectMember_ThrowsAccessDenied() {
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teamMember.getId()))
                .thenReturn(false);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.checkCanViewProject(teamMember, project)
        );

        assertEquals("You don't have access to this project", exception.getMessage());
    }

    @Test
    @DisplayName("Can view project - different team members are checked independently")
    void checkCanViewProject_DifferentTeamMembers_CheckedIndependently() {
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teamMember.getId()))
                .thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(
                project.getId(), otherTeamMember.getId()
        )).thenReturn(false);

        assertDoesNotThrow(() -> projectAccessService.checkCanViewProject(teamMember, project));
        assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.checkCanViewProject(otherTeamMember, project)
        );
    }

    @Test
    @DisplayName("Can modify project - admin - always allowed")
    void checkCanModifyProject_Admin_DoesNotThrow() {
        assertDoesNotThrow(() -> projectAccessService.checkCanModifyProject(admin, project));
    }

    @Test
    @DisplayName("Can modify project - project's own manager - allowed")
    void checkCanModifyProject_ProjectsOwnManager_DoesNotThrow() {
        assertDoesNotThrow(
                () -> projectAccessService.checkCanModifyProject(projectManager, project)
        );
    }

    @Test
    @DisplayName("Can modify project - a different project's manager - denied")
    void checkCanModifyProject_DifferentProjectManager_ThrowsAccessDenied() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.checkCanModifyProject(otherProjectManager, project)
        );

        assertEquals("You don't have permission to modify this project", exception.getMessage());
    }

    @Test
    @DisplayName("Can modify project - team member - always denied, regardless of membership")
    void checkCanModifyProject_TeamMember_ThrowsAccessDeniedWithoutCheckingMembership() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.checkCanModifyProject(teamMember, project)
        );

        assertEquals("You don't have permission to modify this project", exception.getMessage());

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(any(), any());
    }
}
