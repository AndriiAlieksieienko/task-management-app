package com.andrii.taskmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.taskmanagement.dto.user.UserLoginRequestDto;
import com.andrii.taskmanagement.dto.user.UserLoginResponseDto;
import com.andrii.taskmanagement.security.JwtUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private static final String EMAIL = "member@example.com";
    private static final String PASSWORD = "SecurePass123";
    private static final String TOKEN = "generated-jwt-token";

    private UserLoginRequestDto loginDto;

    @BeforeEach
    void setUp() {
        loginDto = new UserLoginRequestDto();
        loginDto.setEmail(EMAIL);
        loginDto.setPassword(PASSWORD);
    }

    @Test
    @DisplayName("Authenticate - valid credentials - returns login response with token")
    void authenticate_ValidCredentials_ReturnLoginResponseWithToken() {
        Authentication successfulAuthentication = new UsernamePasswordAuthenticationToken(
                EMAIL,
                PASSWORD,
                List.of(new SimpleGrantedAuthority("ROLE_TEAM_MEMBER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(successfulAuthentication);
        when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);

        UserLoginResponseDto actual = authenticationService.authenticate(loginDto);

        assertEquals(new UserLoginResponseDto(TOKEN), actual);
    }

    @Test
    @DisplayName("Authenticate - passes email and password to authentication manager")
    void authenticate_ValidCredentials_PassesCredentialsToAuthenticationManager() {
        Authentication successfulAuthentication = new UsernamePasswordAuthenticationToken(
                EMAIL,
                PASSWORD,
                List.of(new SimpleGrantedAuthority("ROLE_TEAM_MEMBER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(successfulAuthentication);
        when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);

        authenticationService.authenticate(loginDto);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager, times(1)).authenticate(captor.capture());

        UsernamePasswordAuthenticationToken submittedToken = captor.getValue();
        assertEquals(EMAIL, submittedToken.getPrincipal());
        assertEquals(PASSWORD, submittedToken.getCredentials());
    }

    @Test
    @DisplayName("Authenticate - generates token using the authenticated principal's name")
    void authenticate_ValidCredentials_GeneratesTokenForAuthenticatedName() {
        // authenticationManager may return an Authentication whose name differs
        // from the raw input (e.g. normalized casing) - the token must be
        // generated from that, not from the original request DTO
        Authentication successfulAuthentication = new UsernamePasswordAuthenticationToken(
                "normalized-" + EMAIL,
                PASSWORD,
                List.of(new SimpleGrantedAuthority("ROLE_TEAM_MEMBER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(successfulAuthentication);
        when(jwtUtil.generateToken("normalized-" + EMAIL)).thenReturn(TOKEN);

        UserLoginResponseDto actual = authenticationService.authenticate(loginDto);

        assertEquals(new UserLoginResponseDto(TOKEN), actual);
        verify(jwtUtil, times(1)).generateToken("normalized-" + EMAIL);
    }

    @Test
    @DisplayName("Authenticate - invalid credentials - propagates authentication exception")
    void authenticate_InvalidCredentials_PropagatesException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.authenticate(loginDto)
        );

        verify(jwtUtil, times(0)).generateToken(any());
    }
}
