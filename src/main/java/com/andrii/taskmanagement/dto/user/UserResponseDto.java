package com.andrii.taskmanagement.dto.user;

import com.andrii.taskmanagement.model.RoleName;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        RoleName role
) {
}
