package com.example.scm.auth;

import com.example.scm.auth.dto.LoginRequest;
import com.example.scm.auth.service.impl.AuthServiceImpl;
import com.example.scm.auth.vo.LoginResponse;
import com.example.scm.auth.vo.UserProfileResponse;
import com.example.scm.common.core.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceImplTest {

    private static final String SECRET = "change-this-dev-secret-key-at-least-32-bytes";

    @Test
    void shouldLoginRefreshAndResolveCurrentUser() {
        AuthServiceImpl authService = new AuthServiceImpl(
                "scm-auth",
                SECRET,
                7200,
                "admin",
                "admin123",
                1L,
                10001L,
                "ROLE_ADMIN,ROLE_TENANT_ADMIN"
        );
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginResponse loginResponse = authService.login(request);

        assertNotNull(loginResponse.getAccessToken());
        assertEquals("Bearer", loginResponse.getTokenType());
        assertEquals(1L, loginResponse.getTenantId());
        assertEquals(10001L, loginResponse.getUserId());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_TENANT_ADMIN"), loginResponse.getRoles());

        String authorizationHeader = "Bearer " + loginResponse.getAccessToken();
        LoginResponse refreshResponse = authService.refresh(authorizationHeader);
        UserProfileResponse profileResponse = authService.currentUser(authorizationHeader);

        assertNotNull(refreshResponse.getAccessToken());
        assertEquals("admin", profileResponse.getUsername());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_TENANT_ADMIN"), profileResponse.getRoles());
        assertDoesNotThrow(() -> authService.logout(authorizationHeader));
    }

    @Test
    void shouldRejectInvalidCredentialAndAuthorizationHeader() {
        AuthServiceImpl authService = new AuthServiceImpl(
                "scm-auth",
                SECRET,
                7200,
                "admin",
                "admin123",
                1L,
                10001L,
                "ROLE_ADMIN"
        );
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("bad-password");

        BusinessException loginException = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("400", loginException.getCode());

        BusinessException authException = assertThrows(BusinessException.class, () -> authService.refresh("bad-token"));
        assertEquals("401", authException.getCode());
        assertTrue(authException.getMessage().contains("Authorization"));
    }
}
