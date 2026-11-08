package com.casemanagement.security;

import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes());

    private final JwtService service = new JwtService(SECRET, 60000, "case-management-test");

    @Test
    void generatedTokenExposesSubjectAndEmailAndIsValid() {
        UUID userId = UUID.randomUUID();
        User user = new User("agent@example.com", "hashed", UserRole.AGENT);
        ReflectionTestUtils.setField(user, "id", userId);

        String token = service.generateAccessToken(user);

        assertTrue(service.isTokenValid(token));
        assertEquals(userId, service.extractUserId(token));
        assertEquals("agent@example.com", service.extractEmail(token));
    }

    @Test
    void tokenFromDifferentIssuerIsInvalid() {
        JwtService otherIssuer = new JwtService(SECRET, 60000, "other-issuer");
        User user = new User("agent@example.com", "hashed", UserRole.AGENT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        assertFalse(service.isTokenValid(otherIssuer.generateAccessToken(user)));
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService expiredTokenService = new JwtService(SECRET, -1000, "case-management-test");
        User user = new User("agent@example.com", "hashed", UserRole.AGENT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        assertFalse(service.isTokenValid(expiredTokenService.generateAccessToken(user)));
    }
}
