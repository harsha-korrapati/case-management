package com.casemanagement.controller;

import com.casemanagement.dto.request.LoginRequest;
import com.casemanagement.dto.response.LoginResponse;
import com.casemanagement.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginDelegatesCredentialsAndReturnsToken() {
        LoginRequest request = new LoginRequest("person@example.com", "password");
        LoginResponse login = new LoginResponse("jwt", "Bearer", 1800);
        when(authService.login(request)).thenReturn(login);

        var response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(login, response.getBody());
        verify(authService).login(request);
    }
}
