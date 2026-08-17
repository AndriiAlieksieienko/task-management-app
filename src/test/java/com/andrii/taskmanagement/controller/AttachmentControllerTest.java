package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.config.CustomMySqlContainer;
import com.andrii.taskmanagement.dto.attachment.AttachmentCreateRequestDto;
import com.andrii.taskmanagement.model.Attachment;
import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectMemberId;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.TaskStatus;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.attachment.AttachmentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Sql(
        scripts = "classpath:database/users/insert-attachment-test-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/users/clean-attachment-test-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class AttachmentControllerTest {
    @Container
    @ServiceConnection
    static final CustomMySqlContainer MYSQL_CONTAINER = CustomMySqlContainer.getInstance();

    private static final String ADMIN_EMAIL = "attachment-test-admin@gmail.com";
    private static final String MANAGER_EMAIL = "attachment-test-manager@gmail.com";
    private static final String MEMBER_EMAIL = "attachment-test-member@gmail.com";
    private static final String SECOND_MEMBER_EMAIL = "attachment-test-member-2@gmail.com";

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

    @Autowired
    private AttachmentRepository attachmentRepository;

    private User admin;
    private User manager;
    private User member;
    private User secondMember;

    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        member = userRepository.findByEmail(MEMBER_EMAIL).orElseThrow();
        secondMember = userRepository.findByEmail(SECOND_MEMBER_EMAIL).orElseThrow();

        project = createProject("Attachment Test Project", manager);
        addMember(project, member);
        task = createTask(project, manager, member, "Task for attachments");
    }

    @Test
    @DisplayName("Create attachment - project manager - returns created attachment")
    void createAttachment_ProjectManager_ReturnCreatedAttachment() throws Exception {
        AttachmentCreateRequestDto requestDto = new AttachmentCreateRequestDto(
                task.getId(),
                "dropbox-file-id-123",
                "design.pdf"
        );

        mockMvc.perform(
                        post("/api/attachments")
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(task.getId()))
                .andExpect(jsonPath("$.uploadedById").value(manager.getId()))
                .andExpect(jsonPath("$.dropboxFileId").value("dropbox-file-id-123"))
                .andExpect(jsonPath("$.filename").value("design.pdf"));

        assertThat(attachmentRepository.findAllByTaskId(task.getId())).hasSize(1);

        Attachment savedAttachment = attachmentRepository.findAllByTaskId(task.getId()).get(0);

        assertThat(savedAttachment.getFilename()).isEqualTo("design.pdf");
        assertThat(savedAttachment.getUploadedBy().getId()).isEqualTo(manager.getId());
        assertThat(savedAttachment.getTask().getId()).isEqualTo(task.getId());
    }

    @Test
    @DisplayName("Create attachment - admin - returns created attachment")
    void createAttachment_Admin_ReturnCreatedAttachment() throws Exception {
        AttachmentCreateRequestDto requestDto = new AttachmentCreateRequestDto(
                task.getId(),
                "dropbox-file-id-456",
                "notes.txt"
        );

        mockMvc.perform(
                        post("/api/attachments")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedById").value(admin.getId()));
    }

    @Test
    @DisplayName("Create attachment - project member without modify rights - forbidden")
    void createAttachment_TeamMember_ReturnsForbidden() throws Exception {
        AttachmentCreateRequestDto requestDto = new AttachmentCreateRequestDto(
                task.getId(),
                "dropbox-file-id-789",
                "screenshot.png"
        );

        mockMvc.perform(
                        post("/api/attachments")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        assertThat(attachmentRepository.findAllByTaskId(task.getId())).isEmpty();
    }

    @Test
    @DisplayName("Create attachment - non-member - forbidden")
    void createAttachment_NonMember_ReturnsForbidden() throws Exception {
        AttachmentCreateRequestDto requestDto = new AttachmentCreateRequestDto(
                task.getId(),
                "dropbox-file-id-999",
                "outsider.png"
        );

        mockMvc.perform(
                        post("/api/attachments")
                                .with(user(SECOND_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        assertThat(attachmentRepository.findAllByTaskId(task.getId())).isEmpty();
    }

    @Test
    @DisplayName("Create attachment - unauthenticated - forbidden")
    void createAttachment_Unauthenticated_ReturnsForbidden() throws Exception {
        AttachmentCreateRequestDto requestDto = new AttachmentCreateRequestDto(
                task.getId(),
                "dropbox-file-id-000",
                "anon.png"
        );

        mockMvc.perform(
                        post("/api/attachments")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Create attachment - nonexistent task - not found")
    void createAttachment_NonexistentTask_ReturnsNotFound() throws Exception {
        AttachmentCreateRequestDto requestDto = new AttachmentCreateRequestDto(
                999999L,
                "dropbox-file-id-123",
                "design.pdf"
        );

        mockMvc.perform(
                        post("/api/attachments")
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create attachment - blank filename - bad request")
    void createAttachment_BlankFilename_ReturnsBadRequest() throws Exception {
        AttachmentCreateRequestDto requestDto = new AttachmentCreateRequestDto(
                task.getId(),
                "dropbox-file-id-123",
                ""
        );

        mockMvc.perform(
                        post("/api/attachments")
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(attachmentRepository.findAllByTaskId(task.getId())).isEmpty();
    }

    @Test
    @DisplayName("Get attachments - project member - returns attachments")
    void getAttachments_ProjectMember_ReturnsAttachments() throws Exception {
        createAttachment(task, manager, "file-1", "one.pdf");
        createAttachment(task, manager, "file-2", "two.pdf");

        mockMvc.perform(
                        get("/api/attachments")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("taskId", task.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Get attachments - non-member - forbidden")
    void getAttachments_NonMember_ReturnsForbidden() throws Exception {
        createAttachment(task, manager, "file-1", "one.pdf");

        mockMvc.perform(
                        get("/api/attachments")
                                .with(user(SECOND_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("taskId", task.getId().toString())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get attachments - nonexistent task - not found")
    void getAttachments_NonexistentTask_ReturnsNotFound() throws Exception {
        mockMvc.perform(
                        get("/api/attachments")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .param("taskId", "999999")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get attachments - unauthenticated - forbidden")
    void getAttachments_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(
                        get("/api/attachments")
                                .param("taskId", task.getId().toString())
                )
                .andExpect(status().isForbidden());
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

    private Task createTask(Project forProject, User creator, User assignee, String name) {
        Task newTask = new Task();

        newTask.setProject(forProject);
        newTask.setCreatedBy(creator);
        newTask.setAssignee(assignee);
        newTask.setName(name);
        newTask.setDescription("Description for " + name);
        newTask.setPriority(Priority.MEDIUM);
        newTask.setStatus(TaskStatus.NOT_STARTED);
        newTask.setDueDate(LocalDate.of(2026, 12, 1));

        LocalDateTime now = LocalDateTime.now();
        newTask.setCreatedAt(now);
        newTask.setUpdatedAt(now);

        return taskRepository.save(newTask);
    }

    private Attachment createAttachment(
            Task forTask,
            User uploader,
            String dropboxFileId,
            String filename
    ) {
        Attachment attachment = new Attachment();

        attachment.setTask(forTask);
        attachment.setUploadedBy(uploader);
        attachment.setDropboxFileId(dropboxFileId);
        attachment.setFilename(filename);
        attachment.setUploadDate(LocalDateTime.now());

        return attachmentRepository.save(attachment);
    }
}
