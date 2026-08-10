package com.andrii.taskmanagement.dto.project;

import java.time.LocalDateTime;

public record ProjectMemberResponseDto(
        Long userId,
        String username,
        String firstName,
        String lastName,
        LocalDateTime joinedAt
) {
}
