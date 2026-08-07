package com.andrii.taskmanagement.service.impl;

import com.andrii.taskmanagement.dto.user.UserRegistrationRequestDto;
import com.andrii.taskmanagement.dto.user.UserResponseDto;
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
import com.andrii.taskmanagement.service.UserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto register(UserRegistrationRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RegistrationException(
                    "Can't register user. Email already exists: " + requestDto.getEmail()
            );
        }
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new RegistrationException(
                    "Can't register user. Username already exists: " + requestDto.getUsername()
            );
        }

        User user = userMapper.toModel(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        Role role = roleRepository.findByName(RoleName.ROLE_TEAM_MEMBER)
                .orElseThrow(() -> new EntityNotFoundException(
                        RoleName.ROLE_TEAM_MEMBER + " not found")
                );
        user.setRole(role);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponseDto getCurrentUser(String email) {
        return userMapper.toUserResponse(getUserByEmail(email));
    }

    @Override
    public UserResponseDto updateUser(String email, UserUpdateRequestDto requestDto) {
        User user = getUserByEmail(email);

        if (!user.getEmail().equals(requestDto.email())
                && userRepository.existsByEmail(requestDto.email())) {
            throw new DuplicateEmailException(
                    "Can't register user. Email already exists: " + requestDto.email()
            );
        }

        userMapper.updateUserFromDto(requestDto, user);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return userMapper.toUserResponse(updatedUser);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found by email: " + email)
                );
    }
}
