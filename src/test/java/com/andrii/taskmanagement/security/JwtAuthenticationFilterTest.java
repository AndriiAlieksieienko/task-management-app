package com.andrii.taskmanagement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String TOKEN = "valid.jwt.token";
    private static final String USERNAME = "member@example.com";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid Bearer token - sets authentication and continues the chain")
    void doFilterInternal_ValidToken_SetsAuthenticationAndContinuesChain() throws Exception {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_TEAM_MEMBER"));

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtUtil.isValidToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getUsername(TOKEN)).thenReturn(USERNAME);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenAnswer(invocation -> authorities);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertEquals(userDetails, authentication.getPrincipal());
        assertEquals(authorities, authentication.getAuthorities());

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Valid token - loads user by the username extracted from the token")
    void doFilterInternal_ValidToken_LoadsUserByUsernameFromToken() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtUtil.isValidToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getUsername(TOKEN)).thenReturn(USERNAME);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenAnswer(invocation -> List.of());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, times(1)).loadUserByUsername(USERNAME);
    }

    @Test
    @DisplayName("No Authorization header - does not set authentication, continues the chain")
    void doFilterInternal_NoAuthorizationHeader_DoesNotSetAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtUtil, never()).isValidToken(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Blank Authorization header - does not set authentication, continues the chain")
    void doFilterInternal_BlankAuthorizationHeader_DoesNotSetAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("   ");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtUtil, never()).isValidToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Header without Bearer prefix - does not set authentication, continues the chain")
    void doFilterInternal_HeaderWithoutBearerPrefix_DoesNotSetAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(TOKEN);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtUtil, never()).isValidToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Header with lowercase 'bearer' prefix - does not set authentication (case-sensitive)")
    void doFilterInternal_LowercaseBearerPrefix_DoesNotSetAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("bearer " + TOKEN);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtUtil, never()).isValidToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid token - does not set authentication, continues the chain")
    void doFilterInternal_InvalidToken_DoesNotSetAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtUtil.isValidToken(TOKEN)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtUtil, never()).getUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Empty Bearer token (header is exactly 'Bearer ') - does not set authentication")
    void doFilterInternal_EmptyBearerToken_DoesNotSetAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer ");
        when(jwtUtil.isValidToken("")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
