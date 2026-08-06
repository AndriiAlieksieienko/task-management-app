package com.andrii.taskmanagement.service;

import com.andrii.taskmanagement.dto.user.UserLoginRequestDto;
import com.andrii.taskmanagement.dto.user.UserLoginResponseDto;

public interface AuthenticationService {
    public UserLoginResponseDto authenticate(UserLoginRequestDto requestDto);
}
