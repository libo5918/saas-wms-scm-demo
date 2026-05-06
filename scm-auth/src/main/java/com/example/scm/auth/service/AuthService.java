package com.example.scm.auth.service;

import com.example.scm.auth.dto.LoginRequest;
import com.example.scm.auth.vo.LoginResponse;
import com.example.scm.auth.vo.UserProfileResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse refresh(String authorizationHeader);

    UserProfileResponse currentUser(String authorizationHeader);

    void logout(String authorizationHeader);
}
