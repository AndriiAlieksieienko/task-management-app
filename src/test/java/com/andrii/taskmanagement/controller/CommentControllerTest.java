package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.config.CustomMySqlContainer;
import com.andrii.taskmanagement.dto.comment.CommentCreateRequestDto;
import com.andrii.taskmanagement.model.Comment;
import com.andrii.taskmanagement.model.Priority;
import com.andrii.taskmanagement.model.Project;
import com.andrii.taskmanagement.model.ProjectMember;
import com.andrii.taskmanagement.model.ProjectMemberId;
import com.andrii.taskmanagement.model.ProjectStatus;
import com.andrii.taskmanagement.model.Task;
import com.andrii.taskmanagement.model.TaskStatus;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.comment.CommentRepository;
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
        scripts = "classpath:database/users/insert-comment-test-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/users/clean-comment-test-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CommentControllerTest {
    @Container
    @ServiceConnection
    static final CustomMySqlContainer MYSQL_CONTAINER = CustomMySqlContainer.getInstance();

    private static final String ADMIN_EMAIL = "comment-test-admin@gmail.com";
    private static final String MANAGER_EMAIL = "comment-test-manager@gmail.com";
    private static final String MEMBER_EMAIL = "comment-test-member@gmail.com";
    private static final String SECOND_MEMBER_EMAIL = "comment-test-member-2@gmail.com";

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
    private CommentRepository commentRepository;

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

        project = createProject("Comment Test Project", manager);
        addMember(project, member);
        task = createTask(project, manager, member, "Task for comments");
    }

    @Test
    @DisplayName("Create comment - project member - returns created comment")
    void createComment_ProjectMember_ReturnCreatedComment() throws Exception {
        CommentCreateRequestDto requestDto = new CommentCreateRequestDto(
                task.getId(),
                "This looks good to me"
        );

        mockMvc.perform(
                        post("/api/comments")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(task.getId()))
                .andExpect(jsonPath("$.userId").value(member.getId()))
                .andExpect(jsonPath("$.text").value("This looks good to me"));

        assertThat(commentRepository.findAllByTaskId(task.getId())).hasSize(1);

        Comment savedComment = commentRepository.findAllByTaskId(task.getId()).get(0);

        assertThat(savedComment.getText()).isEqualTo("This looks good to me");
        assertThat(savedComment.getUser().getId()).isEqualTo(member.getId());
        assertThat(savedComment.getTask().getId()).isEqualTo(task.getId());
    }

    @Test
    @DisplayName("Create comment - admin - returns created comment")
    void createComment_Admin_ReturnCreatedComment() throws Exception {
        CommentCreateRequestDto requestDto = new CommentCreateRequestDto(
                task.getId(),
                "Admin comment"
        );

        mockMvc.perform(
                        post("/api/comments")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(admin.getId()));
    }

    @Test
    @DisplayName("Create comment - non-member - forbidden")
    void createComment_NonMember_ReturnsForbidden() throws Exception {
        CommentCreateRequestDto requestDto = new CommentCreateRequestDto(
                task.getId(),
                "I shouldn't be able to post this"
        );

        mockMvc.perform(
                        post("/api/comments")
                                .with(user(SECOND_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        assertThat(commentRepository.findAllByTaskId(task.getId())).isEmpty();
    }

    @Test
    @DisplayName("Create comment - unauthenticated - forbidden")
    void createComment_Unauthenticated_ReturnsForbidden() throws Exception {
        CommentCreateRequestDto requestDto = new CommentCreateRequestDto(
                task.getId(),
                "Anonymous comment"
        );

        mockMvc.perform(
                        post("/api/comments")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Create comment - nonexistent task - not found")
    void createComment_NonexistentTask_ReturnsNotFound() throws Exception {
        CommentCreateRequestDto requestDto = new CommentCreateRequestDto(
                999999L,
                "Comment on a missing task"
        );

        mockMvc.perform(
                        post("/api/comments")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create comment - blank text - bad request")
    void createComment_BlankText_ReturnsBadRequest() throws Exception {
        CommentCreateRequestDto requestDto = new CommentCreateRequestDto(task.getId(), "");

        mockMvc.perform(
                        post("/api/comments")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(commentRepository.findAllByTaskId(task.getId())).isEmpty();
    }

    @Test
    @DisplayName("Get comments - project member - returns comments")
    void getComments_ProjectMember_ReturnsComments() throws Exception {
        createComment(task, member, "First comment");
        createComment(task, manager, "Second comment");

        mockMvc.perform(
                        get("/api/comments")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("taskId", task.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Get comments - non-member - forbidden")
    void getComments_NonMember_ReturnsForbidden() throws Exception {
        createComment(task, member, "First comment");

        mockMvc.perform(
                        get("/api/comments")
                                .with(user(SECOND_MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .param("taskId", task.getId().toString())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get comments - nonexistent task - not found")
    void getComments_NonexistentTask_ReturnsNotFound() throws Exception {
        mockMvc.perform(
                        get("/api/comments")
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .param("taskId", "999999")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get comments - unauthenticated - forbidden")
    void getComments_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(
                        get("/api/comments")
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

    private Comment createComment(Task forTask, User author, String text) {
        Comment comment = new Comment();

        comment.setTask(forTask);
        comment.setUser(author);
        comment.setText(text);
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }
}
