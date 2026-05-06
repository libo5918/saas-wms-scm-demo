package com.example.scm.common.security;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;

public class JwtTokenProvider {

    private final String issuer;
    private final long expireSeconds;
    private final SecretKey secretKey;

    public JwtTokenProvider(String issuer, String secret, long expireSeconds) {
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
        }
        this.issuer = issuer;
        this.expireSeconds = expireSeconds;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtTokenClaims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtTokenClaims(
                    toLong(claims.get("tenantId"), "tenantId"),
                    toLong(claims.get("userId"), "userId"),
                    claims.get("username", String.class),
                    toRoles(claims.get("roles")),
                    claims.getIssuedAt().toInstant().getEpochSecond(),
                    claims.getExpiration().toInstant().getEpochSecond()
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED.code(), "Invalid token");
        }
    }

    public String generateToken(Long tenantId, Long userId, String username, List<String> roles) {
        long now = Instant.now().getEpochSecond();
        long expireAt = now + expireSeconds;
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("tenantId", tenantId)
                .claim("userId", userId)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(new Date(now * 1000))
                .expiration(new Date(expireAt * 1000))
                .signWith(secretKey)
                .compact();
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    private Long toLong(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED.code(), "Token missing " + fieldName);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private List<String> toRoles(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            return Collections.singletonList(String.valueOf(value));
        }
        List<String> roles = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            roles.add(String.valueOf(item));
        }
        return roles;
    }
}
