package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.config.CustomMySqlContainer;
import com.andrii.taskmanagement.dto.project.ProjectCreateRequestDto;
import com.andrii.taskmanagement.dto.project.ProjectUpdateRequestDto;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectMemberId;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Sql(
        scripts = {
                "classpath:database/projects/clean-project-tables.sql",
                "classpath:database/users/insert-project-test-users.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)

@Sql(
        scripts = "classpath:database/projects/clean-project-tables.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class ProjectControllerTest {
    @Container
    @ServiceConnection
    static final CustomMySqlContainer MYSQL_CONTAINER = CustomMySqlContainer.getInstance();

    private static final String ADMIN_EMAIL = "project-test-admin@gmail.com";
    private static final String PROJECT_MANAGER_EMAIL = "project-test-manager@gmail.com";
    private static final String TEAM_MEMBER_EMAIL = "project-test-member@gmail.com";
    private static final String SECOND_TEAM_MEMBER_EMAIL = "project-test-member-2@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private User admin;
    private User projectManager;
    private User teamMember;
    private User secondTeamMember;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        projectManager = userRepository.findByEmail(PROJECT_MANAGER_EMAIL).orElseThrow();
        teamMember = userRepository.findByEmail(TEAM_MEMBER_EMAIL).orElseThrow();
        secondTeamMember = userRepository.findByEmail(SECOND_TEAM_MEMBER_EMAIL).orElseThrow();
    }

    @Test
    void createProject_ValidRequest_ReturnCreatedProject() throws Exception {
        ProjectCreateRequestDto requestDto = new ProjectCreateRequestDto(
                "Task Management Project",
                "Project for managing tasks",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                projectManager.getId()
        );

        mockMvc.perform(
                        post("/api/projects")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Task Management Project"))
                .andExpect(jsonPath("$.description").value("Project for managing tasks"))
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.createdById").value(admin.getId()))
                .andExpect(jsonPath("$.projectManagerId").value(projectManager.getId()))
                .andExpect(jsonPath("$.members").isEmpty());

        assertThat(projectRepository.findAll()).hasSize(1);

        Project savedProject = projectRepository.findAll().get(0);

        assertThat(savedProject.getName()).isEqualTo("Task Management Project");

        assertThat(savedProject.getCreatedBy().getId()).isEqualTo(admin.getId());

        assertThat(savedProject.getProjectManager().getId()).isEqualTo(projectManager.getId());

        assertThat(savedProject.getStatus()).isEqualTo(ProjectStatus.INITIATED);
    }

    @Test
    void getAll_Admin_ReturnsProjects() throws Exception {
        createProject("Project One", "First project");

        createProject("Project Two", "Second project");

        mockMvc.perform(
                        get("/api/projects")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Project One"))
                .andExpect(jsonPath("$.content[1].name").value("Project Two"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getProjectById_ExistingId_ReturnProject() throws Exception {
        Project project = createProject("Test Project", "Test project description");

        mockMvc.perform(
                        get("/api/projects/{id}", project.getId())
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.description").value("Test project description"))
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.createdById").value(admin.getId()))
                .andExpect(jsonPath("$.projectManagerId").value(projectManager.getId()));
    }

    @Test
    void updateProject_ValidRequest_ReturnUpdatedProject() throws Exception {
        Project project = createProject("Old Project Name", "Old description");

        ProjectUpdateRequestDto requestDto = new ProjectUpdateRequestDto(
                "Updated Project Name",
                "Updated description",
                ProjectStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 1, 31)
        );

        mockMvc.perform(
                        put("/api/projects/{id}", project.getId())
                                .with(user(PROJECT_MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Updated Project Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdById").value(admin.getId()))
                .andExpect(jsonPath("$.projectManagerId").value(projectManager.getId()));

        Project updatedProject = projectRepository
                .findById(project.getId())
                .orElseThrow();

        assertThat(updatedProject.getName()).isEqualTo("Updated Project Name");

        assertThat(updatedProject.getDescription()).isEqualTo("Updated description");

        assertThat(updatedProject.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);

        assertThat(updatedProject.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));

        assertThat(updatedProject.getEndDate()).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    @Test
    void deleteProject_ExistingId_ReturnNoContent() throws Exception {
        Project project = createProject("Project To Delete", "This project will be deleted");

        mockMvc.perform(
                        delete("/api/projects/{id}", project.getId())
                                .with(user(PROJECT_MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                )
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(project.getId())).isEmpty();
    }

    @Test
    @DisplayName("Create project - team member - forbidden")
    void createProject_TeamMember_ReturnsForbidden() throws Exception {
        ProjectCreateRequestDto requestDto = new ProjectCreateRequestDto(
                "Team Member Project",
                "This project should not be created",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                projectManager.getId()
        );

        mockMvc.perform(
                        post("/api/projects")
                                .with(user(TEAM_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Update project - team member - forbidden")
    void updateProject_TeamMember_ReturnsForbidden() throws Exception {
        Project project = createProject("Project", "Original description");

        ProjectUpdateRequestDto requestDto = new ProjectUpdateRequestDto(
                "Updated Project",
                "Updated description",
                ProjectStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 1, 31)
        );

        mockMvc.perform(
                        put("/api/projects/{id}", project.getId())
                                .with(user(TEAM_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        Project unchangedProject = projectRepository
                .findById(project.getId())
                .orElseThrow();

        assertThat(unchangedProject.getName()).isEqualTo("Project");

        assertThat(unchangedProject.getDescription()).isEqualTo("Original description");
    }

    @Test
    @DisplayName("Delete project - team member - forbidden")
    void deleteProject_TeamMember_ReturnsForbidden() throws Exception {
        Project project = createProject("Project To Protect", "This project must remain");

        mockMvc.perform(
                        delete("/api/projects/{id}", project.getId())
                                .with(user(TEAM_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                )
                .andExpect(status().isForbidden());

        assertThat(projectRepository.findById(project.getId())).isPresent();
    }

    @Test
    @DisplayName("Get projects - unauthenticated - unauthorized")
    void getAll_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/projects")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get projects - team member - allowed")
    void getAll_TeamMember_ReturnsProjects() throws Exception {
        createProject("Project One", "First project");

        mockMvc.perform(
                        get("/api/projects")
                                .with(user(TEAM_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get projects - team member with no memberships - returns empty page")
    void getAll_TeamMemberWithNoMemberships_ReturnsEmptyPage()
            throws Exception {

        createProject("Project One", "First project");

        mockMvc.perform(
                        get("/api/projects")
                                .with(user(TEAM_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Get project by ID - team member of project - returns project")
    void getProjectById_TeamMemberOfProject_ReturnsProject() throws Exception {
        Project project = createProject("Team Project", "Project for team member");

        addMember(project, teamMember);

        mockMvc.perform(
                        get("/api/projects/{id}", project.getId())
                                .with(user(TEAM_MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Team Project"))
                .andExpect(jsonPath("$.description").value("Project for team member"))
                .andExpect(jsonPath("$.projectManagerId").value(projectManager.getId()));
    }

    @Test
    @DisplayName("Get project by ID - non-member team member - forbidden")
    void getProjectById_TeamMemberNotMember_ReturnsForbidden()
            throws Exception {

        Project project = createProject("Private Project", "Project without team member");

        mockMvc.perform(
                        get("/api/projects/{id}", project.getId())
                                .with(user(TEAM_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get project by ID - admin - returns project")
    void getProjectById_Admin_ReturnsProject() throws Exception {
        Project project = createProject("Admin Project", "Project accessible by admin");

        mockMvc.perform(
                        get("/api/projects/{id}", project.getId())
                                .with(user(ADMIN_EMAIL)
                                        .roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Admin Project"));
    }

    @Test
    @DisplayName("Get project by ID - project manager - returns project")
    void getProjectById_ProjectManager_ReturnsProject()
            throws Exception {

        Project project = createProject("Manager Project", "Project managed by project manager");

        mockMvc.perform(
                        get("/api/projects/{id}", project.getId())
                                .with(user(PROJECT_MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Manager Project"))
                .andExpect(jsonPath("$.projectManagerId").value(projectManager.getId()));
    }

    @Test
    @DisplayName("Get all projects - team member - returns only member projects")
    void getAll_TeamMember_ReturnsOnlyMemberProjects()
            throws Exception {

        Project projectOne = createProject("Project One", "First project");

        Project projectTwo = createProject("Project Two", "Second project");

        Project projectThree = createProject("Project Three", "Third project");

        addMember(projectOne, teamMember);
        addMember(projectTwo, secondTeamMember);
        addMember(projectThree, teamMember);

        mockMvc.perform(
                        get("/api/projects")
                                .with(user(TEAM_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name").value(
                        containsInAnyOrder("Project One", "Project Three")
                ));
    }

    @Test
    @DisplayName("Create project - blank name - bad request")
    void createProject_BlankName_ReturnsBadRequest() throws Exception {
        ProjectCreateRequestDto requestDto = new ProjectCreateRequestDto(
                "",
                "Description",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                projectManager.getId()
        );

        mockMvc.perform(
                        post("/api/projects")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Create project - no project manager - bad request")
    void createProject_NoProjectManager_ReturnsBadRequest()
            throws Exception {

        ProjectCreateRequestDto requestDto = new ProjectCreateRequestDto(
                "Project",
                "Description",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                null
        );

        mockMvc.perform(
                        post("/api/projects")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Create project - invalid dates - bad request")
    void createProject_InvalidDates_ReturnsBadRequest()
            throws Exception {

        ProjectCreateRequestDto requestDto = new ProjectCreateRequestDto(
                "Invalid Project",
                "Description",
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 8, 1),
                projectManager.getId()
        );

        mockMvc.perform(
                        post("/api/projects")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get project - nonexistent ID - not found")
    void getProjectById_NonexistentId_ReturnsNotFound()
            throws Exception {

        mockMvc.perform(
                        get("/api/projects/{id}", 999999L)
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create project - nonexistent manager - not found")
    void createProject_NonexistentManager_ReturnsNotFound()
            throws Exception {

        ProjectCreateRequestDto requestDto = new ProjectCreateRequestDto(
                "Project",
                "Description",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                999999L
        );

        mockMvc.perform(
                        post("/api/projects")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    private Project createProject(String name, String description) {
        Project project = new Project();

        project.setName(name);
        project.setDescription(description);
        project.setStatus(ProjectStatus.INITIATED);
        project.setStartDate(LocalDate.of(2026, 8, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setCreatedBy(admin);
        project.setProjectManager(projectManager);

        LocalDateTime now = LocalDateTime.now();

        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        return projectRepository.save(project);
    }

    private ProjectMember addMember(Project project, User user) {
        ProjectMember projectMember = new ProjectMember();

        ProjectMemberId id = new ProjectMemberId(
                project.getId(),
                user.getId()
        );

        projectMember.setId(id);
        projectMember.setProject(project);
        projectMember.setUser(user);
        projectMember.setJoinedAt(LocalDateTime.now());

        return projectMemberRepository.save(projectMember);
    }
}
