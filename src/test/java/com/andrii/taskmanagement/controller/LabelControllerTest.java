package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.config.CustomMySqlContainer;
import com.andrii.taskmanagement.dto.label.LabelCreateRequestDto;
import com.andrii.taskmanagement.dto.label.LabelUpdateRequestDto;
import com.andrii.taskmanagement.model.Label;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectMemberId;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.label.LabelRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Sql(
        scripts = "classpath:database/users/insert-user-test-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/clean-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class LabelControllerTest {
    @Container
    @ServiceConnection
    static final CustomMySqlContainer MYSQL_CONTAINER = CustomMySqlContainer.getInstance();

    private static final String ADMIN_EMAIL = "user-test-admin@gmail.com";
    private static final String MANAGER_EMAIL = "user-test-manager@gmail.com";
    private static final String MEMBER_EMAIL = "user-test-member@gmail.com";
    private static final String SECOND_MEMBER_EMAIL = "user-test-member-2@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private LabelRepository labelRepository;

    private User admin;
    private User manager;
    private User member;
    private User secondMember;

    private Project project;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        member = userRepository.findByEmail(MEMBER_EMAIL).orElseThrow();
        secondMember = userRepository.findByEmail(SECOND_MEMBER_EMAIL).orElseThrow();

        project = createProject("Label Test Project", manager);
        addMember(project, member);
    }

    @Test
    @DisplayName("Create label - project manager - returns created label")
    void createLabel_ProjectManager_ReturnCreatedLabel() throws Exception {
        LabelCreateRequestDto requestDto = new LabelCreateRequestDto(
                project.getId(),
                "Bug",
                "#FF0000"
        );

        mockMvc.perform(
                        post("/api/labels")
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Bug"))
                .andExpect(jsonPath("$.color").value("#FF0000"));

        assertThat(labelRepository.findAllByProjectId(project.getId())).hasSize(1);

        Label savedLabel = labelRepository.findAllByProjectId(project.getId()).get(0);

        assertThat(savedLabel.getName()).isEqualTo("Bug");
        assertThat(savedLabel.getColor()).isEqualTo("#FF0000");
        assertThat(savedLabel.getProject().getId()).isEqualTo(project.getId());
    }

    @Test
    @DisplayName("Create label - team member - forbidden")
    void createLabel_TeamMember_ReturnsForbidden() throws Exception {
        LabelCreateRequestDto requestDto = new LabelCreateRequestDto(
                project.getId(),
                "Unauthorized label",
                "#00FF00"
        );

        mockMvc.perform(
                        post("/api/labels")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        assertThat(labelRepository.findAllByProjectId(project.getId())).isEmpty();
    }

    @Test
    @DisplayName("Create label - nonexistent project - not found")
    void createLabel_NonexistentProject_ReturnsNotFound() throws Exception {
        LabelCreateRequestDto requestDto = new LabelCreateRequestDto(
                999999L,
                "Bug",
                "#FF0000"
        );

        mockMvc.perform(
                        post("/api/labels")
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create label - blank name - bad request")
    void createLabel_BlankName_ReturnsBadRequest() throws Exception {
        LabelCreateRequestDto requestDto = new LabelCreateRequestDto(
                project.getId(),
                "",
                "#FF0000"
        );

        mockMvc.perform(
                        post("/api/labels")
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(labelRepository.findAllByProjectId(project.getId())).isEmpty();
    }

    @Test
    @DisplayName("Get labels - project member - returns labels")
    void getLabels_ProjectMember_ReturnsLabels() throws Exception {
        createLabel(project, "Bug", "#FF0000");
        createLabel(project, "Feature", "#00FF00");

        mockMvc.perform(
                        get("/api/labels")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("projectId", project.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Get labels - non-member - forbidden")
    void getLabels_NonMember_ReturnsForbidden() throws Exception {
        createLabel(project, "Bug", "#FF0000");

        mockMvc.perform(
                        get("/api/labels")
                                .with(user(SECOND_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("projectId", project.getId().toString())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get labels - nonexistent project - not found")
    void getLabels_NonexistentProject_ReturnsNotFound() throws Exception {
        mockMvc.perform(
                        get("/api/labels")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .param("projectId", "999999")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update label - project manager - returns updated label")
    void updateLabel_ValidRequest_ReturnUpdatedLabel() throws Exception {
        Label label = createLabel(project, "Old Name", "#000000");

        LabelUpdateRequestDto requestDto = new LabelUpdateRequestDto("New Name", "#FFFFFF");

        mockMvc.perform(
                        put("/api/labels/{id}", label.getId())
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(label.getId()))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.color").value("#FFFFFF"));

        Label updatedLabel = labelRepository.findById(label.getId()).orElseThrow();

        assertThat(updatedLabel.getName()).isEqualTo("New Name");
        assertThat(updatedLabel.getColor()).isEqualTo("#FFFFFF");
    }

    @Test
    @DisplayName("Update label - team member - forbidden")
    void updateLabel_TeamMember_ReturnsForbidden() throws Exception {
        Label label = createLabel(project, "Old Name", "#000000");

        LabelUpdateRequestDto requestDto = new LabelUpdateRequestDto("New Name", "#FFFFFF");

        mockMvc.perform(
                        put("/api/labels/{id}", label.getId())
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        Label unchangedLabel = labelRepository.findById(label.getId()).orElseThrow();
        assertThat(unchangedLabel.getName()).isEqualTo("Old Name");
    }

    @Test
    @DisplayName("Update label - nonexistent id - not found")
    void updateLabel_NonexistentId_ReturnsNotFound() throws Exception {
        LabelUpdateRequestDto requestDto = new LabelUpdateRequestDto("New Name", "#FFFFFF");

        mockMvc.perform(
                        put("/api/labels/{id}", 999999L)
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update label - blank name - bad request")
    void updateLabel_BlankName_ReturnsBadRequest() throws Exception {
        Label label = createLabel(project, "Old Name", "#000000");

        LabelUpdateRequestDto requestDto = new LabelUpdateRequestDto("", "#FFFFFF");

        mockMvc.perform(
                        put("/api/labels/{id}", label.getId())
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Delete label - project manager - returns no content")
    void deleteLabel_ValidId_ReturnsNoContent() throws Exception {
        Label label = createLabel(project, "To Delete", "#123456");

        mockMvc.perform(
                        delete("/api/labels/{id}", label.getId())
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                )
                .andExpect(status().isNoContent());

        assertThat(labelRepository.findById(label.getId())).isEmpty();
    }

    @Test
    @DisplayName("Delete label - team member - forbidden")
    void deleteLabel_TeamMember_ReturnsForbidden() throws Exception {
        Label label = createLabel(project, "To Protect", "#123456");

        mockMvc.perform(
                        delete("/api/labels/{id}", label.getId())
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                )
                .andExpect(status().isForbidden());

        assertThat(labelRepository.findById(label.getId())).isPresent();
    }

    @Test
    @DisplayName("Delete label - nonexistent id - not found")
    void deleteLabel_NonexistentId_ReturnsNotFound() throws Exception {
        mockMvc.perform(
                        delete("/api/labels/{id}", 999999L)
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                )
                .andExpect(status().isNotFound());
    }

    private Project createProject(String name, User manager) {
        Project newProject = new Project();

        newProject.setName(name);
        newProject.setDescription("Description for " + name);
        newProject.setStatus(ProjectStatus.INITIATED);
        newProject.setStartDate(LocalDate.of(2026, 1, 1));
        newProject.setEndDate(LocalDate.of(2026, 12, 31));
        newProject.setCreatedBy(admin);
        newProject.setProjectManager(manager);

        LocalDateTime now = LocalDateTime.now();
        newProject.setCreatedAt(now);
        newProject.setUpdatedAt(now);

        return projectRepository.save(newProject);
    }

    private ProjectMember addMember(Project forProject, User user) {
        ProjectMember projectMember = new ProjectMember();

        ProjectMemberId id = new ProjectMemberId(forProject.getId(), user.getId());

        projectMember.setId(id);
        projectMember.setProject(forProject);
        projectMember.setUser(user);
        projectMember.setJoinedAt(LocalDateTime.now());

        return projectMemberRepository.save(projectMember);
    }

    private Label createLabel(Project forProject, String name, String color) {
        Label label = new Label();

        label.setProject(forProject);
        label.setName(name);
        label.setColor(color);
        label.setCreatedAt(LocalDateTime.now());

        return labelRepository.save(label);
    }
}
