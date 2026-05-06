package com.example.scm.auth.service.impl;

import com.example.scm.auth.dto.LoginRequest;
import com.example.scm.auth.service.AuthService;
import com.example.scm.auth.vo.LoginResponse;
import com.example.scm.auth.vo.UserProfileResponse;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.security.JwtTokenClaims;
import com.example.scm.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final String demoUsername;
    private final String demoPassword;
    private final Long demoTenantId;
    private final Long demoUserId;
    private final List<String> demoRoles;

    public AuthServiceImpl(@Value("${auth.jwt.issuer:scm-auth}") String issuer,
                           @Value("${auth.jwt.secret}") String secret,
                           @Value("${auth.jwt.expire-seconds:7200}") long expireSeconds,
                           @Value("${auth.demo-user.username:admin}") String demoUsername,
                           @Value("${auth.demo-user.password:admin123}") String demoPassword,
                           @Value("${auth.demo-user.tenant-id:1}") Long demoTenantId,
                           @Value("${auth.demo-user.user-id:10001}") Long demoUserId,
                           @Value("${auth.demo-user.roles:ROLE_ADMIN,ROLE_TENANT_ADMIN}") String demoRoles) {
        this.jwtTokenProvider = new JwtTokenProvider(issuer, secret, expireSeconds);
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
        this.demoTenantId = demoTenantId;
        this.demoUserId = demoUserId;
        this.demoRoles = Arrays.stream(demoRoles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (!demoUsername.equals(request.getUsername()) || !demoPassword.equals(request.getPassword())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Invalid username or password");
        }
        return buildLoginResponse(demoTenantId, demoUserId, demoUsername, demoRoles);
    }

    @Override
    public LoginResponse refresh(String authorizationHeader) {
        JwtTokenClaims claims = parseAuthorizationHeader(authorizationHeader);
        return buildLoginResponse(claims.tenantId(), claims.userId(), claims.username(), claims.roles());
    }

    @Override
    public UserProfileResponse currentUser(String authorizationHeader) {
        JwtTokenClaims claims = parseAuthorizationHeader(authorizationHeader);
        UserProfileResponse response = new UserProfileResponse();
        response.setTenantId(claims.tenantId());
        response.setUserId(claims.userId());
        response.setUsername(claims.username());
        response.setRoles(claims.roles());
        return response;
    }

    @Override
    public void logout(String authorizationHeader) {
        parseAuthorizationHeader(authorizationHeader);
    }

    private JwtTokenClaims parseAuthorizationHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED.code(), "Missing or invalid Authorization header");
        }
        return jwtTokenProvider.parseToken(authorizationHeader.substring(7));
    }

    private LoginResponse buildLoginResponse(Long tenantId, Long userId, String username, List<String> roles) {
        String token = jwtTokenProvider.generateToken(tenantId, userId, username, roles);
        JwtTokenClaims claims = jwtTokenProvider.parseToken(token);
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresAt(claims.expiresAtEpochSecond());
        response.setTenantId(tenantId);
        response.setUserId(userId);
        response.setUsername(username);
        response.setRoles(roles);
        return response;
    }
}
