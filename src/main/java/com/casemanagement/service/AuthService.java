package com.casemanagement.service;

import com.casemanagement.dto.request.LoginRequest;
import com.casemanagement.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
