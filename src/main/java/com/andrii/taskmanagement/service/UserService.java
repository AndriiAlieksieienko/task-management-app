package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.user.UserRegistrationRequestDto;
import com.andrii.taskmanagement.dto.user.UserResponseDto;
import com.andrii.taskmanagement.dto.user.UserRoleUpdateRequestDto;
import com.andrii.taskmanagement.dto.user.UserUpdateRequestDto;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto requestDto);

    UserResponseDto getCurrentUser(String email);

    UserResponseDto updateUser(String email, UserUpdateRequestDto requestDto);

    UserResponseDto updateUserRole(Long id, UserRoleUpdateRequestDto requestDto);
}
