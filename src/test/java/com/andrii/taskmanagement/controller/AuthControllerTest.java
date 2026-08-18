package com.andrii.taskmanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andrii.taskmanagement.config.CustomMySqlContainer;
import com.andrii.taskmanagement.dto.user.UserLoginRequestDto;
import com.andrii.taskmanagement.dto.user.UserRegistrationRequestDto;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerTest {
    @Container
    @ServiceConnection
    static final CustomMySqlContainer MYSQL_CONTAINER = CustomMySqlContainer.getInstance();

    private static final String REGISTER_EMAIL = "auth-test-register@example.com";
    private static final String DUPLICATE_EMAIL = "auth-test-duplicate@example.com";
    private static final String DUPLICATE_USERNAME_EMAIL = "auth-test-duplicate-username@example.com";
    private static final String LOGIN_EMAIL = "auth-test-login@example.com";
    private static final String VALID_PASSWORD = "SecurePass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM users WHERE email IN (?, ?, ?, ?, ?)",
                REGISTER_EMAIL,
                DUPLICATE_EMAIL,
                DUPLICATE_USERNAME_EMAIL,
                LOGIN_EMAIL,
                "auth-test-other-email@example.com"
        );
    }

    @Test
    @DisplayName("Register user - valid request - returns created user")
    void registerUser_ValidRequest_ReturnCreatedUser() throws Exception {
        UserRegistrationRequestDto requestDto = registrationDto(
                "authTestRegister", REGISTER_EMAIL, VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(REGISTER_EMAIL))
                .andExpect(jsonPath("$.email").value(REGISTER_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.role").value(RoleName.ROLE_TEAM_MEMBER.name()));

        assertThat(userRepository.findByEmail(REGISTER_EMAIL)).isPresent();
        assertThat(userRepository.findByEmail(REGISTER_EMAIL).get().getRole().getName())
                .isEqualTo(RoleName.ROLE_TEAM_MEMBER);
    }

    @Test
    @DisplayName("Register user - duplicate email - returns conflict")
    void registerUser_DuplicateEmail_ReturnsConflict() throws Exception {
        UserRegistrationRequestDto firstRequest = registrationDto(
                "authTestDup1", DUPLICATE_EMAIL, VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(firstRequest))
                )
                .andExpect(status().isOk());

        UserRegistrationRequestDto secondRequest = registrationDto(
                "authTestDup2", DUPLICATE_EMAIL, VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(secondRequest))
                )
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Register user - duplicate username - returns conflict")
    void registerUser_DuplicateUsername_ReturnsConflict() throws Exception {
        UserRegistrationRequestDto firstRequest = registrationDto(
                "authTestDupUsername", DUPLICATE_USERNAME_EMAIL, VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(firstRequest))
                )
                .andExpect(status().isOk());

        UserRegistrationRequestDto secondRequest = registrationDto(
                "authTestDupUsername", "auth-test-other-email@example.com", VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(secondRequest))
                )
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Register user - passwords don't match - returns bad request")
    void registerUser_PasswordMismatch_ReturnsBadRequest() throws Exception {
        UserRegistrationRequestDto requestDto = registrationDto(
                "authTestMismatch", REGISTER_EMAIL, VALID_PASSWORD
        );
        requestDto.setRepeatPassword("DifferentPass123");

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail(REGISTER_EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("Register user - blank username - returns bad request")
    void registerUser_BlankUsername_ReturnsBadRequest() throws Exception {
        UserRegistrationRequestDto requestDto = registrationDto("", REGISTER_EMAIL, VALID_PASSWORD);

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail(REGISTER_EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("Register user - invalid email format - returns bad request")
    void registerUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        UserRegistrationRequestDto requestDto = registrationDto(
                "authTestInvalidEmail", "not-an-email", VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register user - password too short - returns bad request")
    void registerUser_ShortPassword_ReturnsBadRequest() throws Exception {
        UserRegistrationRequestDto requestDto = registrationDto(
                "authTestShortPass", REGISTER_EMAIL, "short"
        );
        requestDto.setRepeatPassword("short");

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail(REGISTER_EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("Login - valid credentials - returns token")
    void login_ValidCredentials_ReturnsToken() throws Exception {
        UserRegistrationRequestDto registrationDto = registrationDto(
                "authTestLogin", LOGIN_EMAIL, VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registrationDto))
                )
                .andExpect(status().isOk());

        UserLoginRequestDto loginDto = new UserLoginRequestDto();
        loginDto.setEmail(LOGIN_EMAIL);
        loginDto.setPassword(VALID_PASSWORD);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Login - wrong password - returns forbidden")
    void login_WrongPassword_ReturnsForbidden() throws Exception {
        UserRegistrationRequestDto registrationDto = registrationDto(
                "authTestWrongPass", LOGIN_EMAIL, VALID_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/registration")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registrationDto))
                )
                .andExpect(status().isOk());

        UserLoginRequestDto loginDto = new UserLoginRequestDto();
        loginDto.setEmail(LOGIN_EMAIL);
        loginDto.setPassword("WrongPassword123");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDto))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login - nonexistent email - returns forbidden")
    void login_NonexistentEmail_ReturnsForbidden() throws Exception {
        UserLoginRequestDto loginDto = new UserLoginRequestDto();
        loginDto.setEmail("does-not-exist@example.com");
        loginDto.setPassword(VALID_PASSWORD);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDto))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login - blank fields - returns bad request")
    void login_BlankFields_ReturnsBadRequest() throws Exception {
        UserLoginRequestDto loginDto = new UserLoginRequestDto();
        loginDto.setEmail("");
        loginDto.setPassword("");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDto))
                )
                .andExpect(status().isBadRequest());
    }

    private UserRegistrationRequestDto registrationDto(
            String username,
            String email,
            String password
    ) {
        UserRegistrationRequestDto dto = new UserRegistrationRequestDto();

        dto.setUsername(username);
        dto.setPassword(password);
        dto.setRepeatPassword(password);
        dto.setEmail(email);
        dto.setFirstName("Test");
        dto.setLastName("User");

        return dto;
    }
}
