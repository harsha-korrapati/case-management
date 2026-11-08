package com.casemanagement.service.impl;

import com.casemanagement.dto.request.LoginRequest;
import com.casemanagement.dto.response.LoginResponse;
import com.casemanagement.security.CustomUserDetails;
import com.casemanagement.security.JwtService;
import com.casemanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.security.jwt.expiration-ms}")
    private long expirationMs;

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(),
                                request.password()
                        )
                );

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(
                // We will use the authenticated user entity in the next step.
                userDetails.getUser()
        );

        return new LoginResponse(
                accessToken,
                "Bearer",
                expirationMs / 1000
        );
    }
}
