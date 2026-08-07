package com.andrii.taskmanagement.dto.user;

import com.andrii.taskmanagement.model.RoleName;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequestDto(
        @NotNull
        RoleName role
) {
}
