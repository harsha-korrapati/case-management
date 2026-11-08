package com.casemanagement.config;

import com.casemanagement.ratelimit.RateLimitFilter;
import com.casemanagement.security.CustomAccessDeniedHandler;
import com.casemanagement.security.JwtAuthenticationEntryPoint;
import com.casemanagement.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void passwordEncoderHashesAndVerifiesPasswords() {
        SecurityConfig config = config();
        PasswordEncoder encoder = config.passwordEncoder();

        String encoded = encoder.encode("a-strong-password");

        assertTrue(encoder.matches("a-strong-password", encoded));
        assertFalse(encoder.matches("wrong-password", encoded));
    }

    @Test
    void authenticationManagerComesFromAuthenticationConfiguration() throws Exception {
        SecurityConfig config = config();
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(manager);

        assertSame(manager, config.authenticationManager(authenticationConfiguration));
    }

    private SecurityConfig config() {
        return new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(JwtAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class),
                mock(RateLimitFilter.class));
    }

    private static void assertSame(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
    }
}
