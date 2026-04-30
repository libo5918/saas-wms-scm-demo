package com.example.scm.auth.service;

import com.example.scm.auth.dto.LoginRequest;
import com.example.scm.auth.vo.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
