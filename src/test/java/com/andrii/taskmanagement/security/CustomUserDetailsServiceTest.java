package com.andrii.taskmanagement.security;

import com.andrii.taskmanagement.model.Role;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private static final String EMAIL = "member@example.com";

    private User user;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setId(1L);
        role.setName(RoleName.ROLE_TEAM_MEMBER);

        user = new User();
        user.setId(1L);
        user.setUsername("teamMember");
        user.setEmail(EMAIL);
        user.setPassword("encoded-password");
        user.setRole(role);
    }

    @Test
    @DisplayName("Load user by username - existing email - returns user details")
    void loadUserByUsername_ExistingEmail_ReturnsUserDetails() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails actual = customUserDetailsService.loadUserByUsername(EMAIL);

        assertEquals(user, actual);
        assertEquals(EMAIL, actual.getUsername());
    }

    @Test
    @DisplayName("Load user by username - looks up by email despite the parameter name")
    void loadUserByUsername_LooksUpByEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        customUserDetailsService.loadUserByUsername(EMAIL);

        verify(userRepository, times(1)).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Load user by username - nonexistent email - throws exception")
    void loadUserByUsername_NonexistentEmail_ThrowsException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(EMAIL)
        );

        assertEquals("User not found with login: " + EMAIL, exception.getMessage());
    }
}
