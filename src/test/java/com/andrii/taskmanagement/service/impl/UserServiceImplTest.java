package com.andrii.taskmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.taskmanagement.dto.user.UserRegistrationRequestDto;
import com.andrii.taskmanagement.dto.user.UserResponseDto;
import com.andrii.taskmanagement.dto.user.UserRoleUpdateRequestDto;
import com.andrii.taskmanagement.dto.user.UserUpdateRequestDto;
import com.andrii.taskmanagement.exception.DuplicateEmailException;
import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.exception.RegistrationException;
import com.andrii.taskmanagement.mapper.UserMapper;
import com.andrii.taskmanagement.model.Role;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.user.RoleRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String EMAIL = "member@example.com";
    private static final String RAW_PASSWORD = "SecurePass123";
    private static final String ENCODED_PASSWORD = "encoded-password-hash";

    private Role teamMemberRole;
    private Role projectManagerRole;

    private User user;

    private UserRegistrationRequestDto registrationDto;
    private UserUpdateRequestDto updateDto;
    private UserRoleUpdateRequestDto roleUpdateDto;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        teamMemberRole = new Role();
        teamMemberRole.setId(1L);
        teamMemberRole.setName(RoleName.ROLE_TEAM_MEMBER);

        projectManagerRole = new Role();
        projectManagerRole.setId(2L);
        projectManagerRole.setName(RoleName.ROLE_PROJECT_MANAGER);

        registrationDto = new UserRegistrationRequestDto();
        registrationDto.setUsername("member");
        registrationDto.setPassword(RAW_PASSWORD);
        registrationDto.setRepeatPassword(RAW_PASSWORD);
        registrationDto.setEmail(EMAIL);
        registrationDto.setFirstName("Team");
        registrationDto.setLastName("Member");

        updateDto = new UserUpdateRequestDto("UpdatedFirst", "UpdatedLast", EMAIL);
        roleUpdateDto = new UserRoleUpdateRequestDto(RoleName.ROLE_PROJECT_MANAGER);

        user = new User();
        user.setId(1L);
        user.setUsername("member");
        user.setEmail(EMAIL);
        user.setFirstName("Team");
        user.setLastName("Member");
        user.setRole(teamMemberRole);

        userResponseDto = new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName()
        );
    }

    @Test
    @DisplayName("Register - valid request - returns user response")
    void register_ValidRequest_ReturnUserResponseDto() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername("member")).thenReturn(false);
        when(userMapper.toModel(registrationDto)).thenReturn(user);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(roleRepository.findByName(RoleName.ROLE_TEAM_MEMBER))
                .thenReturn(Optional.of(teamMemberRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(userResponseDto);

        UserResponseDto actual = userService.register(registrationDto);

        assertEquals(userResponseDto, actual);
        assertEquals(ENCODED_PASSWORD, user.getPassword());
        assertEquals(teamMemberRole, user.getRole());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Register - duplicate email - throws exception")
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        RegistrationException exception = assertThrows(
                RegistrationException.class,
                () -> userService.register(registrationDto)
        );

        assertEquals(
                "Can't register user. Email already exists: " + EMAIL,
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register - duplicate username - throws exception")
    void register_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername("member")).thenReturn(true);

        RegistrationException exception = assertThrows(
                RegistrationException.class,
                () -> userService.register(registrationDto)
        );

        assertEquals(
                "Can't register user. Username already exists: member",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register - default role not found - throws exception")
    void register_RoleNotFound_ThrowsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername("member")).thenReturn(false);
        when(userMapper.toModel(registrationDto)).thenReturn(user);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(roleRepository.findByName(RoleName.ROLE_TEAM_MEMBER)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.register(registrationDto)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get current user - existing email - returns user response")
    void getCurrentUser_ExistingEmail_ReturnUserResponseDto() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponseDto);

        UserResponseDto actual = userService.getCurrentUser(EMAIL);

        assertEquals(userResponseDto, actual);
    }

    @Test
    @DisplayName("Get current user - nonexistent email - throws exception")
    void getCurrentUser_NonexistentEmail_ThrowsException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.getCurrentUser(EMAIL)
        );

        assertEquals("User not found by email: " + EMAIL, exception.getMessage());
    }

    @Test
    @DisplayName("Update user - valid request - returns updated user response")
    void updateUser_ValidRequest_ReturnUpdatedUserResponseDto() {
        UserUpdateRequestDto newEmailDto =
                new UserUpdateRequestDto("UpdatedFirst", "UpdatedLast", "new@example.com");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(userResponseDto);

        UserResponseDto actual = userService.updateUser(EMAIL, newEmailDto);

        assertEquals(userResponseDto, actual);

        verify(userMapper, times(1)).updateUserFromDto(newEmailDto, user);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Update user - same email - does not check for duplicates")
    void updateUser_SameEmail_DoesNotCheckDuplicates() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(userResponseDto);

        userService.updateUser(EMAIL, updateDto);

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Update user - duplicate email - throws exception")
    void updateUser_DuplicateEmail_ThrowsException() {
        UserUpdateRequestDto newEmailDto =
                new UserUpdateRequestDto("UpdatedFirst", "UpdatedLast", "taken@example.com");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> userService.updateUser(EMAIL, newEmailDto)
        );

        assertEquals(
                "Can't register user. Email already exists: taken@example.com",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user - user not found - throws exception")
    void updateUser_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.updateUser(EMAIL, updateDto)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user role - different role - returns updated user response")
    void updateUserRole_DifferentRole_ReturnUpdatedUserResponseDto() {
        UserResponseDto updatedResponseDto = new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                RoleName.ROLE_PROJECT_MANAGER
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_PROJECT_MANAGER))
                .thenReturn(Optional.of(projectManagerRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(updatedResponseDto);

        UserResponseDto actual = userService.updateUserRole(user.getId(), roleUpdateDto);

        assertEquals(updatedResponseDto, actual);
        assertEquals(projectManagerRole, user.getRole());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Update user role - same role - returns unchanged user without saving")
    void updateUserRole_SameRole_ReturnsUnchangedUserWithoutSaving() {
        UserRoleUpdateRequestDto sameRoleDto = new UserRoleUpdateRequestDto(RoleName.ROLE_TEAM_MEMBER);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_TEAM_MEMBER))
                .thenReturn(Optional.of(teamMemberRole));
        when(userMapper.toUserResponse(user)).thenReturn(userResponseDto);

        UserResponseDto actual = userService.updateUserRole(user.getId(), sameRoleDto);

        assertEquals(userResponseDto, actual);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user role - user not found - throws exception")
    void updateUserRole_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.updateUserRole(999L, roleUpdateDto)
        );

        assertEquals("Can't find the user by id: 999", exception.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user role - role not found - throws exception")
    void updateUserRole_RoleNotFound_ThrowsException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_PROJECT_MANAGER)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.updateUserRole(user.getId(), roleUpdateDto)
        );

        assertEquals(
                "Role not found by name: " + RoleName.ROLE_PROJECT_MANAGER,
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
    }
}
