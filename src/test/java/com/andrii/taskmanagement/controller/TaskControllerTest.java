package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.config.CustomMySqlContainer;
import com.andrii.taskmanagement.dto.task.TaskAssignedUpdateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskCreateRequestDto;
import com.andrii.taskmanagement.dto.task.TaskUpdateRequestDto;
import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectMemberId;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.TaskStatus;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.project.ProjectMemberRepository;
import com.andrii.taskmanagement.repository.project.ProjectRepository;
import com.andrii.taskmanagement.repository.task.TaskRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

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
class TaskControllerTest {
    @Container
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
    private TaskRepository taskRepository;

    private User admin;
    private User manager;
    private User member;
    private User secondMember;

    private Project project;

    @BeforeEach
    void setUp() {
        admin = userRepository
                .findByEmail(ADMIN_EMAIL)
                .orElseThrow();

        manager = userRepository
                .findByEmail(MANAGER_EMAIL)
                .orElseThrow();

        member = userRepository
                .findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        secondMember = userRepository
                .findByEmail(SECOND_MEMBER_EMAIL)
                .orElseThrow();

        project = createProject("Task Test Project", manager);

        addMember(project, member);
    }

    @Test
    @DisplayName("Create task - project manager - returns created task")
    void createTask_ValidRequest_ReturnCreatedTask() throws Exception {

        TaskCreateRequestDto requestDto = new TaskCreateRequestDto(
                project.getId(),
                member.getId(),
                "Implement login page",
                "Build the login screen",
                Priority.HIGH,
                TaskStatus.NOT_STARTED,
                LocalDate.of(2026, 9, 1),
                null
        );

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId")
                        .value(project.getId()))
                .andExpect(jsonPath("$.assigneeId")
                        .value(member.getId()))
                .andExpect(jsonPath("$.createdById")
                        .value(manager.getId()))
                .andExpect(jsonPath("$.name")
                        .value("Implement login page"))
                .andExpect(jsonPath("$.description")
                        .value("Build the login screen"))
                .andExpect(jsonPath("$.priority")
                        .value("HIGH"))
                .andExpect(jsonPath("$.status")
                        .value("NOT_STARTED"))
                .andExpect(jsonPath("$.labelIds")
                        .isEmpty());

        assertThat(taskRepository.findAll())
                .hasSize(1);

        Task savedTask = taskRepository
                .findAll()
                .get(0);

        assertThat(savedTask.getName())
                .isEqualTo("Implement login page");

        assertThat(savedTask.getProject().getId())
                .isEqualTo(project.getId());

        assertThat(savedTask.getAssignee().getId())
                .isEqualTo(member.getId());

        assertThat(savedTask.getCreatedBy().getId())
                .isEqualTo(manager.getId());
    }

    @Test
    @DisplayName("Create task - team member - forbidden")
    void createTask_TeamMember_ReturnsForbidden() throws Exception {

        TaskCreateRequestDto requestDto = new TaskCreateRequestDto(
                project.getId(),
                null,
                "Unauthorized task",
                null,
                Priority.LOW,
                TaskStatus.NOT_STARTED,
                null,
                null
        );

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        assertThat(taskRepository.findAll())
                .isEmpty();
    }

    @Test
    @DisplayName("Create task - nonexistent project - not found")
    void createTask_NonexistentProject_ReturnsNotFound()
            throws Exception {

        TaskCreateRequestDto requestDto = new TaskCreateRequestDto(
                999999L,
                null,
                "Task for missing project",
                null,
                Priority.LOW,
                TaskStatus.NOT_STARTED,
                null,
                null
        );

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create task - nonexistent assignee - not found")
    void createTask_NonexistentAssignee_ReturnsNotFound()
            throws Exception {

        TaskCreateRequestDto requestDto = new TaskCreateRequestDto(
                project.getId(),
                999999L,
                "Task with missing assignee",
                null,
                Priority.LOW,
                TaskStatus.NOT_STARTED,
                null,
                null
        );

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create task - nonexistent label - not found")
    void createTask_NonexistentLabel_ReturnsNotFound()
            throws Exception {

        TaskCreateRequestDto requestDto = new TaskCreateRequestDto(
                project.getId(),
                null,
                "Task with missing label",
                null,
                Priority.LOW,
                TaskStatus.NOT_STARTED,
                null,
                Set.of(999999L)
        );

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create task - blank name - bad request")
    void createTask_BlankName_ReturnsBadRequest()
            throws Exception {

        TaskCreateRequestDto requestDto = new TaskCreateRequestDto(
                project.getId(),
                null,
                "",
                null,
                Priority.LOW,
                TaskStatus.NOT_STARTED,
                null,
                null
        );

        mockMvc.perform(
                        post("/api/tasks")
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(taskRepository.findAll())
                .isEmpty();
    }

    @Test
    @DisplayName("Get task by id - project member - returns task")
    void getTaskById_ProjectMember_ReturnsTask()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "Fix bug",
                TaskStatus.IN_PROGRESS
        );

        mockMvc.perform(
                        get("/api/tasks/{id}", task.getId())
                                .with(user(MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(task.getId()))
                .andExpect(jsonPath("$.name")
                        .value("Fix bug"))
                .andExpect(jsonPath("$.status")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.projectId")
                        .value(project.getId()));
    }

    @Test
    @DisplayName("Get task by id - non-member - forbidden")
    void getTaskById_NonMember_ReturnsForbidden()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "Fix bug",
                TaskStatus.IN_PROGRESS
        );

        mockMvc.perform(
                        get("/api/tasks/{id}", task.getId())
                                .with(user(SECOND_MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get task by id - nonexistent id - not found")
    void getTaskById_NonexistentId_ReturnsNotFound()
            throws Exception {

        mockMvc.perform(
                        get("/api/tasks/{id}", 999999L)
                                .with(user(ADMIN_EMAIL)
                                        .roles("ADMIN"))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get tasks for project - project member - returns page")
    void getTasks_ProjectMember_ReturnsPage()
            throws Exception {

        createTask(
                project,
                manager,
                member,
                "Task One",
                TaskStatus.NOT_STARTED
        );

        createTask(
                project,
                manager,
                member,
                "Task Two",
                TaskStatus.NOT_STARTED
        );

        mockMvc.perform(
                        get("/api/tasks")
                                .with(user(MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                                .param(
                                        "projectId",
                                        project.getId().toString()
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(2))
                .andExpect(jsonPath("$.totalElements")
                        .value(2));
    }

    @Test
    @DisplayName("Get tasks for project - non-member - forbidden")
    void getTasks_NonMember_ReturnsForbidden()
            throws Exception {

        createTask(
                project,
                manager,
                member,
                "Task One",
                TaskStatus.NOT_STARTED
        );

        mockMvc.perform(
                        get("/api/tasks")
                                .with(user(SECOND_MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                                .param(
                                        "projectId",
                                        project.getId().toString()
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Update task - project manager - returns updated task")
    void updateTask_ValidRequest_ReturnUpdatedTask()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "Old Name",
                TaskStatus.NOT_STARTED
        );

        TaskUpdateRequestDto requestDto =
                new TaskUpdateRequestDto(
                        secondMember.getId(),
                        "New Name",
                        "New description",
                        Priority.MEDIUM,
                        TaskStatus.IN_PROGRESS,
                        LocalDate.of(2026, 10, 1),
                        null
                );

        mockMvc.perform(
                        put("/api/tasks/{id}", task.getId())
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                requestDto
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(task.getId()))
                .andExpect(jsonPath("$.name")
                        .value("New Name"))
                .andExpect(jsonPath("$.description")
                        .value("New description"))
                .andExpect(jsonPath("$.priority")
                        .value("MEDIUM"))
                .andExpect(jsonPath("$.status")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assigneeId")
                        .value(secondMember.getId()));

        Task updatedTask = taskRepository
                .findById(task.getId())
                .orElseThrow();

        assertThat(updatedTask.getName())
                .isEqualTo("New Name");

        assertThat(updatedTask.getStatus())
                .isEqualTo(TaskStatus.IN_PROGRESS);

        assertThat(updatedTask.getAssignee().getId())
                .isEqualTo(secondMember.getId());
    }

    @Test
    @DisplayName("Update task - team member - forbidden")
    void updateTask_TeamMember_ReturnsForbidden()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "Old Name",
                TaskStatus.NOT_STARTED
        );

        TaskUpdateRequestDto requestDto =
                new TaskUpdateRequestDto(
                        null,
                        "New Name",
                        null,
                        Priority.LOW,
                        TaskStatus.IN_PROGRESS,
                        null,
                        null
                );

        mockMvc.perform(
                        put("/api/tasks/{id}", task.getId())
                                .with(user(MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                requestDto
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        Task unchangedTask = taskRepository
                .findById(task.getId())
                .orElseThrow();

        assertThat(unchangedTask.getName())
                .isEqualTo("Old Name");
    }

    @Test
    @DisplayName("Update task - nonexistent id - not found")
    void updateTask_NonexistentId_ReturnsNotFound()
            throws Exception {

        TaskUpdateRequestDto requestDto =
                new TaskUpdateRequestDto(
                        null,
                        "New Name",
                        null,
                        Priority.LOW,
                        TaskStatus.IN_PROGRESS,
                        null,
                        null
                );

        mockMvc.perform(
                        put("/api/tasks/{id}", 999999L)
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                requestDto
                                        )
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update assigned task - assignee - returns updated task")
    void updateAssignedTask_Assignee_ReturnUpdatedTask()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "My Task",
                TaskStatus.NOT_STARTED
        );

        TaskAssignedUpdateRequestDto requestDto =
                new TaskAssignedUpdateRequestDto(
                        "Updated by assignee",
                        TaskStatus.COMPLETED,
                        LocalDate.of(2026, 11, 1)
                );

        mockMvc.perform(
                        put(
                                "/api/tasks/{id}/assigned",
                                task.getId()
                        )
                                .with(user(MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                requestDto
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.description")
                        .value("Updated by assignee"));

        Task updatedTask = taskRepository
                .findById(task.getId())
                .orElseThrow();

        assertThat(updatedTask.getStatus())
                .isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    @DisplayName("Update assigned task - not the assignee - forbidden")
    void updateAssignedTask_NotAssignee_ReturnsForbidden()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "My Task",
                TaskStatus.NOT_STARTED
        );

        TaskAssignedUpdateRequestDto requestDto =
                new TaskAssignedUpdateRequestDto(
                        "Trying to sneak an update in",
                        TaskStatus.COMPLETED,
                        null
                );

        mockMvc.perform(
                        put(
                                "/api/tasks/{id}/assigned",
                                task.getId()
                        )
                                .with(user(SECOND_MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                requestDto
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        Task unchangedTask = taskRepository
                .findById(task.getId())
                .orElseThrow();

        assertThat(unchangedTask.getStatus())
                .isEqualTo(TaskStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("Update assigned task - project manager role - forbidden")
    void updateAssignedTask_ProjectManagerRole_ReturnsForbidden()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "My Task",
                TaskStatus.NOT_STARTED
        );

        TaskAssignedUpdateRequestDto requestDto =
                new TaskAssignedUpdateRequestDto(
                        null,
                        TaskStatus.COMPLETED,
                        null
                );

        mockMvc.perform(
                        put(
                                "/api/tasks/{id}/assigned",
                                task.getId()
                        )
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                requestDto
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Delete task - project manager - returns no content")
    void deleteTask_ValidId_ReturnsNoContent()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "To Delete",
                TaskStatus.NOT_STARTED
        );

        mockMvc.perform(
                        delete("/api/tasks/{id}", task.getId())
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                )
                .andExpect(status().isNoContent());

        assertThat(taskRepository.findById(task.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Delete task - team member - forbidden")
    void deleteTask_TeamMember_ReturnsForbidden()
            throws Exception {

        Task task = createTask(
                project,
                manager,
                member,
                "To Protect",
                TaskStatus.NOT_STARTED
        );

        mockMvc.perform(
                        delete("/api/tasks/{id}", task.getId())
                                .with(user(MEMBER_EMAIL)
                                        .roles("TEAM_MEMBER"))
                )
                .andExpect(status().isForbidden());

        assertThat(taskRepository.findById(task.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("Delete task - nonexistent id - not found")
    void deleteTask_NonexistentId_ReturnsNotFound()
            throws Exception {

        mockMvc.perform(
                        delete("/api/tasks/{id}", 999999L)
                                .with(user(MANAGER_EMAIL)
                                        .roles("PROJECT_MANAGER"))
                )
                .andExpect(status().isNotFound());
    }

    private Project createProject(
            String name,
            User projectManager
    ) {
        Project newProject = new Project();

        newProject.setName(name);
        newProject.setDescription("Description for " + name);
        newProject.setStatus(ProjectStatus.INITIATED);
        newProject.setStartDate(LocalDate.of(2026, 1, 1));
        newProject.setEndDate(LocalDate.of(2026, 12, 31));
        newProject.setCreatedBy(admin);
        newProject.setProjectManager(projectManager);

        LocalDateTime now = LocalDateTime.now();

        newProject.setCreatedAt(now);
        newProject.setUpdatedAt(now);

        return projectRepository.save(newProject);
    }

    private ProjectMember addMember(
            Project project,
            User user
    ) {
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

    private Task createTask(
            Project project,
            User creator,
            User assignee,
            String name,
            TaskStatus status
    ) {
        Task task = new Task();

        task.setProject(project);
        task.setCreatedBy(creator);
        task.setAssignee(assignee);
        task.setName(name);
        task.setDescription("Description for " + name);
        task.setPriority(Priority.MEDIUM);
        task.setStatus(status);
        task.setDueDate(LocalDate.of(2026, 12, 1));

        LocalDateTime now = LocalDateTime.now();

        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        return taskRepository.save(task);
    }
}
