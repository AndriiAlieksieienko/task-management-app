package com.andrii.taskmanagement.controller;

import com.andrii.taskmanagement.dto.user.UserResponseDto;
import com.andrii.taskmanagement.dto.user.UserUpdateRequestDto;
import com.andrii.taskmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User management", description = "Endpoints for managing users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "See user's profile")
    public UserResponseDto getCurrentUser(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName());
    }

    @PutMapping("/me")
    @Operation(summary = "Update user's profile")
    public UserResponseDto updateUser(
            Authentication authentication,
            @RequestBody @Valid UserUpdateRequestDto requestDto
    ) {
        return userService.updateUser(authentication.getName(), requestDto);
    }
}
