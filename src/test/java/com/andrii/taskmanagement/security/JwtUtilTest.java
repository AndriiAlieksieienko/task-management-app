package com.andrii.taskmanagement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {
    private static final String SECRET =
            "test-secret-key-at-least-32-bytes-long-for-hs256";
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final String USERNAME = "member@example.com";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", ONE_HOUR_MS);
    }

    @Test
    @DisplayName("Generate token - returns non-blank three-part JWT")
    void generateToken_ReturnsNonBlankThreePartToken() {
        String token = jwtUtil.generateToken(USERNAME);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Generate token - sets expiration according to configured value")
    void generateToken_SetsExpirationAccordingToConfiguredValue() {
        long before = System.currentTimeMillis();
        String token = jwtUtil.generateToken(USERNAME);
        long after = System.currentTimeMillis();

        SecretKey secret = (SecretKey) ReflectionTestUtils.getField(jwtUtil, "secret");

        Date expiration = Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        long toleranceMs = 1_000L;

        assertTrue(expiration.getTime() >= before + ONE_HOUR_MS - toleranceMs);
        assertTrue(expiration.getTime() <= after + ONE_HOUR_MS + toleranceMs);
    }

    @Test
    @DisplayName("Get username - valid token - returns original username")
    void getUsername_ValidToken_ReturnsOriginalUsername() {
        String token = jwtUtil.generateToken(USERNAME);

        String actual = jwtUtil.getUsername(token);

        assertEquals(USERNAME, actual);
    }

    @Test
    @DisplayName("Get username - expired token - throws exception")
    void getUsername_ExpiredToken_ThrowsException() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -ONE_HOUR_MS);
        String expiredToken = jwtUtil.generateToken(USERNAME);

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtUtil.getUsername(expiredToken)
        );
    }

    @Test
    @DisplayName("Get username - malformed token - throws exception")
    void getUsername_MalformedToken_ThrowsException() {
        assertThrows(
                JwtException.class,
                () -> jwtUtil.getUsername("not-a-valid-jwt")
        );
    }

    @Test
    @DisplayName("Get username - token signed with a different key - throws exception")
    void getUsername_WrongSigningKey_ThrowsException() {
        String tokenSignedByAnotherKey = tokenSignedWithDifferentKey(USERNAME);

        assertThrows(
                JwtException.class,
                () -> jwtUtil.getUsername(tokenSignedByAnotherKey)
        );
    }

    @Test
    @DisplayName("Is valid token - freshly generated token - returns true")
    void isValidToken_FreshlyGeneratedToken_ReturnsTrue() {
        String token = jwtUtil.generateToken(USERNAME);

        assertTrue(jwtUtil.isValidToken(token));
    }

    @Test
    @DisplayName("Is valid token - expired token - returns false")
    void isValidToken_ExpiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -ONE_HOUR_MS);
        String expiredToken = jwtUtil.generateToken(USERNAME);

        assertFalse(jwtUtil.isValidToken(expiredToken));
    }

    @Test
    @DisplayName("Is valid token - malformed token - returns false")
    void isValidToken_MalformedToken_ReturnsFalse() {
        assertFalse(jwtUtil.isValidToken("not-a-valid-jwt"));
    }

    @Test
    @DisplayName("Is valid token - blank token - returns false")
    void isValidToken_BlankToken_ReturnsFalse() {
        assertFalse(jwtUtil.isValidToken(""));
    }

    @Test
    @DisplayName("Is valid token - token signed with a different key - returns false")
    void isValidToken_WrongSigningKey_ReturnsFalse() {
        String tokenSignedByAnotherKey = tokenSignedWithDifferentKey(USERNAME);

        assertFalse(jwtUtil.isValidToken(tokenSignedByAnotherKey));
    }

    @Test
    @DisplayName("Is valid token - tampered payload - returns false")
    void isValidToken_TamperedPayload_ReturnsFalse() {
        String token = jwtUtil.generateToken(USERNAME);
        String[] parts = token.split("\\.");

        // Swap in a different (still validly base64url-encoded) payload while
        // keeping the original header and signature, simulating an attacker
        // trying to change the subject without re-signing.
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(
                        "{\"sub\":\"attacker@example.com\"}".getBytes(StandardCharsets.UTF_8)
                );
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtUtil.isValidToken(tamperedToken));
    }

    private String tokenSignedWithDifferentKey(String username) {
        SecretKey differentSecret = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-32-bytes!".getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ONE_HOUR_MS))
                .signWith(differentSecret)
                .compact();
    }
}
