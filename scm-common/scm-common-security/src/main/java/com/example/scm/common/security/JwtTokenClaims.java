package com.example.scm.common.security;

import java.util.List;

public record JwtTokenClaims(
        Long tenantId,
        Long userId,
        String username,
        List<String> roles,
        long issuedAtEpochSecond,
        long expiresAtEpochSecond
) {
}
