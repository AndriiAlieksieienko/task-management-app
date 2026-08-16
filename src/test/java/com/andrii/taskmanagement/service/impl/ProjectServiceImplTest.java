package com.andrii.taskmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectManagerUpdateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectMemberResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectResponseDto;
import com.andrii.taskmanagement.dto.project.ProjectSearchParameters;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.mapper.ProjectMapper;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.Role;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.andrii.taskmanagement.service.ProjectAccessService;
import com.andrii.taskmanagement.specification.ProjectSpecificationBuilder;
import java.time.LocalDate;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectSpecificationBuilder projectSpecificationBuilder;
    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private static final String CREATOR_EMAIL = "admin@example.com";
    private static final String MANAGER_EMAIL = "manager@example.com";
    private static final String MEMBER_EMAIL = "member@example.com";

    private Role adminRole;
    private Role projectManagerRole;
    private Role teamMemberRole;

    private User admin;
    private User manager;
    private User teamMember;

    private Project project;
    private ProjectResponseDto projectResponseDto;
    private ProjectCreateRequestDto createDto;
    private ProjectUpdateRequestDto updateDto;
    private ProjectManagerUpdateRequestDto managerUpdateDto;

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
        admin.setEmail(CREATOR_EMAIL);
        admin.setUsername("admin");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setRole(adminRole);

        manager = new User();
        manager.setId(2L);
        manager.setEmail(MANAGER_EMAIL);
        manager.setUsername("manager");
        manager.setFirstName("Project");
        manager.setLastName("Manager");
        manager.setRole(projectManagerRole);

        teamMember = new User();
        teamMember.setId(3L);
        teamMember.setEmail(MEMBER_EMAIL);
        teamMember.setUsername("member");
        teamMember.setFirstName("Team");
        teamMember.setLastName("Member");
        teamMember.setRole(teamMemberRole);

        createDto = new ProjectCreateRequestDto(
                "Task Management Project",
                "Project for managing tasks",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                manager.getId()
        );

        updateDto = new ProjectUpdateRequestDto(
                "Updated Project",
                "Updated description",
                ProjectStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 1, 31)
        );

        managerUpdateDto = new ProjectManagerUpdateRequestDto(manager.getId());

        project = new Project();
        project.setId(1L);
        project.setName(createDto.name());
        project.setDescription(createDto.description());
        project.setStatus(ProjectStatus.INITIATED);
        project.setStartDate(createDto.startDate());
        project.setEndDate(createDto.endDate());
        project.setCreatedBy(admin);
        project.setProjectManager(manager);

        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        projectResponseDto = new ProjectResponseDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                admin.getId(),
                manager.getId(),
                List.of(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    @Test
    @DisplayName("Save valid project")
    void save_ValidRequestDto_ReturnProjectResponseDto() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndRoleName(manager.getId(), RoleName.ROLE_PROJECT_MANAGER))
                .thenReturn(Optional.of(manager));
        when(projectMapper.toModel(createDto)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toProjectResponse(project, List.of())).thenReturn(projectResponseDto);

        ProjectResponseDto actual = projectService.save(createDto, CREATOR_EMAIL);

        assertEquals(projectResponseDto, actual);
        assertEquals(ProjectStatus.INITIATED, project.getStatus());
        assertEquals(admin, project.getCreatedBy());
        assertEquals(manager, project.getProjectManager());

        verify(projectRepository, times(1)).save(project);
    }

    @Test
    @DisplayName("Save project - creator not found - throws exception")
    void save_CreatorNotFound_ThrowsException() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.save(createDto, CREATOR_EMAIL)
        );

        assertEquals("User not found by email: " + CREATOR_EMAIL, exception.getMessage());

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save project - project manager not found - throws exception")
    void save_ProjectManagerNotFound_ThrowsException() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndRoleName(manager.getId(), RoleName.ROLE_PROJECT_MANAGER))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.save(createDto, CREATOR_EMAIL)
        );

        assertEquals(
                "Project manager not found by id: " + manager.getId(),
                exception.getMessage()
        );

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Find project by id - authorized user - returns project")
    void findById_ExistingIdAuthorizedUser_ReturnProjectResponseDto() {
        ProjectMember projectMember = buildProjectMember(project, teamMember);
        ProjectMemberResponseDto memberDto = buildMemberResponse(teamMember);

        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(teamMember));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findAllByProjectIdsWithUsers(List.of(project.getId())))
                .thenReturn(List.of(projectMember));
        when(projectMapper.toProjectMemberResponse(projectMember)).thenReturn(memberDto);
        when(projectMapper.toProjectResponse(project, List.of(memberDto)))
                .thenReturn(projectResponseDto);

        ProjectResponseDto actual = projectService.findById(project.getId(), MEMBER_EMAIL);

        assertEquals(projectResponseDto, actual);

        verify(projectAccessService, times(1)).checkCanViewProject(teamMember, project);
    }

    @Test
    @DisplayName("Find project by id - nonexistent id - throws exception")
    void findById_NonexistentId_ThrowsException() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(admin));
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.findById(999L, CREATOR_EMAIL)
        );

        assertEquals("Can't find the project by id: 999", exception.getMessage());
    }

    @Test
    @DisplayName("Find project by id - access denied - throws exception")
    void findById_AccessDenied_ThrowsException() {
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(teamMember));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        // NOTE: replace RuntimeException with the real exception type thrown by
        // your ProjectAccessService implementation (e.g. AccessDeniedException).
        doThrow(new RuntimeException("Access denied"))
                .when(projectAccessService).checkCanViewProject(teamMember, project);

        assertThrows(
                RuntimeException.class,
                () -> projectService.findById(project.getId(), MEMBER_EMAIL)
        );

        verify(projectMemberRepository, never()).findAllByProjectIdsWithUsers(anyList());
    }

    @Test
    @DisplayName("Find all projects - admin - returns page")
    void findAll_Admin_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ProjectSearchParameters searchParameters = new ProjectSearchParameters(
                null, null, null, null, null, null, null
        );
        Page<Project> projectPage = new PageImpl<>(List.of(project));

        Specification<Project> baseSpecification = Specification.where(null);

        ProjectMember projectMember = buildProjectMember(project, teamMember);
        ProjectMemberResponseDto memberDto = buildMemberResponse(teamMember);

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(admin));
        when(projectSpecificationBuilder.build(searchParameters)).thenReturn(baseSpecification);
        when(projectRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(projectPage);
        when(projectMemberRepository.findAllByProjectIdsWithUsers(List.of(project.getId())))
                .thenReturn(List.of(projectMember));
        when(projectMapper.toProjectMemberResponse(projectMember)).thenReturn(memberDto);
        when(projectMapper.toProjectResponse(project, List.of(memberDto)))
                .thenReturn(projectResponseDto);

        Page<ProjectResponseDto> actual =
                projectService.findAll(searchParameters, pageable, CREATOR_EMAIL);

        assertEquals(1, actual.getTotalElements());
        assertEquals(projectResponseDto, actual.getContent().get(0));

        verify(projectRepository, times(1)).findAll(eq(baseSpecification), eq(pageable));
    }

    @Test
    @DisplayName("Find all projects - project manager - filters by managed projects")
    void findAll_ProjectManager_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ProjectSearchParameters searchParameters = new ProjectSearchParameters(
                null, null, null, null, null, null, null
        );
        Page<Project> projectPage = new PageImpl<>(List.of(project));

        Specification<Project> baseSpecification = Specification.where(null);

        when(userRepository.findByEmail(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
        when(projectSpecificationBuilder.build(searchParameters)).thenReturn(baseSpecification);
        when(projectRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(projectPage);
        when(projectMemberRepository.findAllByProjectIdsWithUsers(List.of(project.getId())))
                .thenReturn(List.of());
        when(projectMapper.toProjectResponse(project, List.of())).thenReturn(projectResponseDto);

        Page<ProjectResponseDto> actual =
                projectService.findAll(searchParameters, pageable, MANAGER_EMAIL);

        assertEquals(1, actual.getTotalElements());

        verify(projectRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Find all projects - empty result - skips member lookup")
    void findAll_EmptyResult_SkipsMemberLookup() {
        Pageable pageable = PageRequest.of(0, 10);
        ProjectSearchParameters searchParameters = new ProjectSearchParameters(
                null, null, null, null, null, null, null
        );
        Page<Project> emptyPage = new PageImpl<>(List.of());

        Specification<Project> baseSpecification = Specification.where(null);

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(admin));
        when(projectSpecificationBuilder.build(searchParameters)).thenReturn(baseSpecification);
        when(projectRepository.findAll(eq(baseSpecification), eq(pageable)))
                .thenReturn(emptyPage);

        Page<ProjectResponseDto> actual =
                projectService.findAll(searchParameters, pageable, CREATOR_EMAIL);

        assertEquals(0, actual.getTotalElements());

        verify(projectMemberRepository, never()).findAllByProjectIdsWithUsers(anyList());
    }

    @Test
    @DisplayName("Update project - valid request - returns updated project")
    void updateById_ValidRequest_ReturnUpdatedProjectResponseDto() {
        ProjectMember projectMember = buildProjectMember(project, teamMember);
        ProjectMemberResponseDto memberDto = buildMemberResponse(teamMember);

        when(userRepository.findByEmail(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMemberRepository.findAllByProjectIdsWithUsers(List.of(project.getId())))
                .thenReturn(List.of(projectMember));
        when(projectMapper.toProjectMemberResponse(projectMember)).thenReturn(memberDto);
        when(projectMapper.toProjectResponse(project, List.of(memberDto)))
                .thenReturn(projectResponseDto);

        ProjectResponseDto actual =
                projectService.updateById(project.getId(), updateDto, MANAGER_EMAIL);

        assertEquals(projectResponseDto, actual);

        verify(projectAccessService, times(1)).checkCanModifyProject(manager, project);
        verify(projectMapper, times(1)).updateProjectFromDto(updateDto, project);
        verify(projectRepository, times(1)).save(project);
    }

    @Test
    @DisplayName("Update project - nonexistent id - throws exception")
    void updateById_NonexistentId_ThrowsException() {
        when(userRepository.findByEmail(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectService.updateById(999L, updateDto, MANAGER_EMAIL)
        );

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete project - valid id - calls repository delete")
    void deleteById_ValidId_CallsRepositoryDelete() {
        when(userRepository.findByEmail(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        projectService.deleteById(project.getId(), MANAGER_EMAIL);

        verify(projectAccessService, times(1)).checkCanModifyProject(manager, project);
        verify(projectRepository, times(1)).delete(project);
    }

    @Test
    @DisplayName("Delete project - nonexistent id - throws exception")
    void deleteById_NonexistentId_ThrowsException() {
        when(userRepository.findByEmail(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectService.deleteById(999L, MANAGER_EMAIL)
        );

        verify(projectRepository, never()).delete((Project) any());
    }

    @Test
    @DisplayName("Update project manager - valid request - returns updated project")
    void updateProjectManager_ValidRequest_ReturnUpdatedProjectResponseDto() {
        User newManager = new User();
        newManager.setId(5L);
        newManager.setEmail("new-manager@example.com");
        newManager.setUsername("newManager");
        newManager.setFirstName("New");
        newManager.setLastName("Manager");
        newManager.setRole(projectManagerRole);

        ProjectManagerUpdateRequestDto newManagerDto =
                new ProjectManagerUpdateRequestDto(newManager.getId());

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByIdAndRoleName(
                newManager.getId(), RoleName.ROLE_PROJECT_MANAGER
        )).thenReturn(Optional.of(newManager));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMemberRepository.findAllByProjectIdsWithUsers(List.of(project.getId())))
                .thenReturn(List.of());
        when(projectMapper.toProjectResponse(project, List.of())).thenReturn(projectResponseDto);

        ProjectResponseDto actual =
                projectService.updateProjectManager(project.getId(), newManagerDto);

        assertEquals(projectResponseDto, actual);
        assertEquals(newManager, project.getProjectManager());

        verify(projectRepository, times(1)).save(project);
    }

    @Test
    @DisplayName("Update project manager - manager not found - throws exception")
    void updateProjectManager_ManagerNotFound_ThrowsException() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByIdAndRoleName(
                managerUpdateDto.projectManagerId(), RoleName.ROLE_PROJECT_MANAGER
        )).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.updateProjectManager(project.getId(), managerUpdateDto)
        );

        assertEquals(
                "Project manager not found by id: " + managerUpdateDto.projectManagerId(),
                exception.getMessage()
        );

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update project manager - nonexistent project - throws exception")
    void updateProjectManager_NonexistentProject_ThrowsException() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectService.updateProjectManager(999L, managerUpdateDto)
        );

        verify(projectRepository, never()).save(any());
    }

    private ProjectMember buildProjectMember(Project forProject, User user) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.setProject(forProject);
        projectMember.setUser(user);
        projectMember.setJoinedAt(LocalDateTime.now());
        return projectMember;
    }

    private ProjectMemberResponseDto buildMemberResponse(User user) {
        return new ProjectMemberResponseDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                LocalDateTime.now()
        );
    }
}
