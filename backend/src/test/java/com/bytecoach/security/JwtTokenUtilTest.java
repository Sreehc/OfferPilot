package com.bytecoach.security;

import com.bytecoach.security.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;

    private static final String TEST_SECRET = "AReallyLongSecretKeyForTestingPurposesOnly12345678901234567890";
    private static final long TEST_EXPIRE = 3600; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpireSeconds", TEST_EXPIRE);
    }

    @Test
    void generateToken_returnsNonNull() {
        String token = jwtTokenUtil.generateToken(1L, "testuser");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void validateToken_freshTokenIsValid() {
        String token = jwtTokenUtil.generateToken(1L, "testuser");
        assertTrue(jwtTokenUtil.validateToken(token));
    }

    @Test
    void validateToken_expiredTokenIsInvalid() throws InterruptedException {
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpireSeconds", 1L);
        String token = jwtTokenUtil.generateToken(1L, "testuser");
        Thread.sleep(1500);
        assertFalse(jwtTokenUtil.validateToken(token));
    }

    @Test
    void parseClaims_returnsCorrectSubject() {
        String token = jwtTokenUtil.generateToken(42L, "alice");
        Claims claims = jwtTokenUtil.parseClaims(token);

        assertEquals("alice", claims.getSubject());
        assertEquals(42L, claims.get("userId", Long.class));
    }

    @Test
    void resolveToken_extractsTokenFromBearerHeader() {
        String result = jwtTokenUtil.resolveToken("Bearer my-jwt-token");
        assertEquals("my-jwt-token", result);
    }

    @Test
    void resolveToken_returnsNullForInvalidHeader() {
        assertNull(jwtTokenUtil.resolveToken(null));
        assertNull(jwtTokenUtil.resolveToken("Basic abc"));
        assertNull(jwtTokenUtil.resolveToken(""));
    }

    @Test
    void validateToken_invalidTokenIsInvalid() {
        assertFalse(jwtTokenUtil.validateToken("not-a-valid-token"));
    }

    @Test
    void generateToken_differentUsersGetDifferentTokens() {
        String token1 = jwtTokenUtil.generateToken(1L, "user1");
        String token2 = jwtTokenUtil.generateToken(2L, "user2");
        assertNotEquals(token1, token2);
    }
}
