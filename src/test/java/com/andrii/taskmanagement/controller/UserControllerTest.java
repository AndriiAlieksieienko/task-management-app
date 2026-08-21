package com.andrii.taskmanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andrii.taskmanagement.config.CustomMySqlContainer;
import com.andrii.taskmanagement.dto.user.UserRoleUpdateRequestDto;
import com.andrii.taskmanagement.dto.user.UserUpdateRequestDto;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Sql(
        scripts = "classpath:database/users/insert-user-test-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/users/remove-user-test-users.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class UserControllerTest {
    @Container
    @ServiceConnection
    static final CustomMySqlContainer MYSQL_CONTAINER = CustomMySqlContainer.getInstance();

    private static final String ADMIN_EMAIL = "user-test-admin@gmail.com";
    private static final String MANAGER_EMAIL = "user-test-manager@gmail.com";
    private static final String MEMBER_EMAIL = "user-test-member@gmail.com";
    private static final String TARGET_EMAIL = "user-test-target@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User manager;
    private User member;
    private User target;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        member = userRepository.findByEmail(MEMBER_EMAIL).orElseThrow();
        target = userRepository.findByEmail(TARGET_EMAIL).orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM users WHERE username IN (?, ?, ?, ?)",
                "userTestAdmin", "userTestManager", "userTestMember", "userTestTarget"
        );
    }

    @Test
    @DisplayName("Get current user - authenticated - returns current user")
    void getCurrentUser_Authenticated_ReturnsCurrentUser() throws Exception {
        mockMvc.perform(
                        get("/users/me")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.email").value(MEMBER_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Team"))
                .andExpect(jsonPath("$.lastName").value("Member"))
                .andExpect(jsonPath("$.role").value(RoleName.ROLE_TEAM_MEMBER.name()));
    }

    @Test
    @DisplayName("Get current user - unauthenticated - forbidden")
    void getCurrentUser_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Update user - valid request - returns updated user")
    void updateUser_ValidRequest_ReturnUpdatedUser() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "UpdatedFirst",
                "UpdatedLast",
                "user-test-member-updated@gmail.com"
        );

        mockMvc.perform(
                        put("/users/me")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedFirst"))
                .andExpect(jsonPath("$.lastName").value("UpdatedLast"))
                .andExpect(jsonPath("$.email").value("user-test-member-updated@gmail.com"));

        User updatedUser = userRepository.findByEmail("user-test-member-updated@gmail.com")
                .orElseThrow();

        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(updatedUser.getLastName()).isEqualTo("UpdatedLast");
    }

    @Test
    @DisplayName("Update user - same email - returns updated user")
    void updateUser_SameEmail_ReturnUpdatedUser() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "UpdatedFirst",
                "UpdatedLast",
                MEMBER_EMAIL
        );

        mockMvc.perform(
                        put("/users/me")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(MEMBER_EMAIL));
    }

    @Test
    @DisplayName("Update user - duplicate email - returns conflict")
    void updateUser_DuplicateEmail_ReturnsConflict() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "UpdatedFirst",
                "UpdatedLast",
                ADMIN_EMAIL
        );

        mockMvc.perform(
                        put("/users/me")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isConflict());

        assertThat(userRepository.findByEmail(MEMBER_EMAIL)).isPresent();
    }

    @Test
    @DisplayName("Update user - blank first name - bad request")
    void updateUser_BlankFirstName_ReturnsBadRequest() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "",
                "UpdatedLast",
                MEMBER_EMAIL
        );

        mockMvc.perform(
                        put("/users/me")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update user - invalid email format - bad request")
    void updateUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "UpdatedFirst",
                "UpdatedLast",
                "not-an-email"
        );

        mockMvc.perform(
                        put("/users/me")
                                .with(user(MEMBER_EMAIL).roles("TEAM_MEMBER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update user - unauthenticated - forbidden")
    void updateUser_Unauthenticated_ReturnsForbidden() throws Exception {
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto(
                "UpdatedFirst",
                "UpdatedLast",
                MEMBER_EMAIL
        );

        mockMvc.perform(
                        put("/users/me")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Update user role - admin - returns updated user")
    void updateUserRole_Admin_ReturnUpdatedUser() throws Exception {
        UserRoleUpdateRequestDto requestDto =
                new UserRoleUpdateRequestDto(RoleName.ROLE_PROJECT_MANAGER);

        mockMvc.perform(
                        put("/users/{id}/role", target.getId())
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(RoleName.ROLE_PROJECT_MANAGER.name()));

        User updatedTarget = userRepository.findByEmail(TARGET_EMAIL).orElseThrow();
        assertThat(updatedTarget.getRole().getName()).isEqualTo(RoleName.ROLE_PROJECT_MANAGER);
    }

    @Test
    @DisplayName("Update user role - same role - returns unchanged user")
    void updateUserRole_SameRole_ReturnsUnchangedUser() throws Exception {
        UserRoleUpdateRequestDto requestDto =
                new UserRoleUpdateRequestDto(RoleName.ROLE_TEAM_MEMBER);

        mockMvc.perform(
                        put("/users/{id}/role", target.getId())
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(RoleName.ROLE_TEAM_MEMBER.name()));
    }

    @Test
    @DisplayName("Update user role - non-admin - forbidden")
    void updateUserRole_NonAdmin_ReturnsForbidden() throws Exception {
        UserRoleUpdateRequestDto requestDto =
                new UserRoleUpdateRequestDto(RoleName.ROLE_PROJECT_MANAGER);

        mockMvc.perform(
                        put("/users/{id}/role", target.getId())
                                .with(user(MANAGER_EMAIL).roles("PROJECT_MANAGER"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());

        User unchangedTarget = userRepository.findByEmail(TARGET_EMAIL).orElseThrow();
        assertThat(unchangedTarget.getRole().getName()).isEqualTo(RoleName.ROLE_TEAM_MEMBER);
    }

    @Test
    @DisplayName("Update user role - nonexistent user - not found")
    void updateUserRole_NonexistentUser_ReturnsNotFound() throws Exception {
        UserRoleUpdateRequestDto requestDto =
                new UserRoleUpdateRequestDto(RoleName.ROLE_PROJECT_MANAGER);

        mockMvc.perform(
                        put("/users/{id}/role", 999999L)
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update user role - null role - bad request")
    void updateUserRole_NullRole_ReturnsBadRequest() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(
                        put("/users/{id}/role", target.getId())
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update user role - unauthenticated - forbidden")
    void updateUserRole_Unauthenticated_ReturnsForbidden() throws Exception {
        UserRoleUpdateRequestDto requestDto =
                new UserRoleUpdateRequestDto(RoleName.ROLE_PROJECT_MANAGER);

        mockMvc.perform(
                        put("/users/{id}/role", target.getId())
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isForbidden());
    }
}
