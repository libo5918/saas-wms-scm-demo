package com.example.scm.auth.controller;

import com.example.scm.auth.dto.LoginRequest;
import com.example.scm.auth.service.AuthService;
import com.example.scm.auth.vo.LoginResponse;
import com.example.scm.auth.vo.UserProfileResponse;
import com.example.scm.common.core.Result;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return Result.success(authService.refresh(authorizationHeader));
    }

    @GetMapping("/me")
    public Result<UserProfileResponse> currentUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return Result.success(authService.currentUser(authorizationHeader));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return Result.success(null);
    }
}
