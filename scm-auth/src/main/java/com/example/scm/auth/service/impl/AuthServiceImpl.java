package com.example.scm.auth.service.impl;

import com.example.scm.auth.dto.LoginRequest;
import com.example.scm.auth.service.AuthService;
import com.example.scm.auth.vo.LoginResponse;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    private final String issuer;
    private final String secret;
    private final long expireSeconds;
    private final String demoUsername;
    private final String demoPassword;
    private final Long demoTenantId;
    private final Long demoUserId;

    public AuthServiceImpl(@Value("${auth.jwt.issuer:scm-auth}") String issuer,
                           @Value("${auth.jwt.secret}") String secret,
                           @Value("${auth.jwt.expire-seconds:7200}") long expireSeconds,
                           @Value("${auth.demo-user.username:admin}") String demoUsername,
                           @Value("${auth.demo-user.password:admin123}") String demoPassword,
                           @Value("${auth.demo-user.tenant-id:1}") Long demoTenantId,
                           @Value("${auth.demo-user.user-id:10001}") Long demoUserId) {
        this.issuer = issuer;
        this.secret = secret;
        this.expireSeconds = expireSeconds;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
        this.demoTenantId = demoTenantId;
        this.demoUserId = demoUserId;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (!demoUsername.equals(request.getUsername()) || !demoPassword.equals(request.getPassword())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Invalid username or password");
        }
        long now = Instant.now().getEpochSecond();
        long expireAt = now + expireSeconds;
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(demoUserId))
                .claim("tenantId", demoTenantId)
                .claim("userId", demoUserId)
                .claim("username", demoUsername)
                .issuedAt(new Date(now * 1000))
                .expiration(new Date(expireAt * 1000))
                .signWith(key)
                .compact();
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresAt(expireAt);
        response.setTenantId(demoTenantId);
        response.setUserId(demoUserId);
        response.setUsername(demoUsername);
        return response;
    }
}
