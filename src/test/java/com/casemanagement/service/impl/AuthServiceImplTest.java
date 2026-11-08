package com.casemanagement.service.impl;

import com.casemanagement.dto.request.LoginRequest;
import com.casemanagement.dto.response.LoginResponse;
import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import com.casemanagement.security.CustomUserDetails;
import com.casemanagement.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expirationMs", 1800000L);
    }

    @Test
    void loginAuthenticatesRequestAndReturnsBearerToken() {
        User user = new User("user@example.com", "hashed", UserRole.AGENT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(user));
        when(jwtService.generateAccessToken(user)).thenReturn("signed-token");

        LoginResponse response = service.login(new LoginRequest("user@example.com", "password"));

        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1800L, response.expiresIn());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateAccessToken(user);
    }
}
