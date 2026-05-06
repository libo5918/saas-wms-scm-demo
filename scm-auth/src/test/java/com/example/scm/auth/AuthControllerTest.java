package com.example.scm.auth;

import com.example.scm.auth.config.SecurityConfig;
import com.example.scm.auth.controller.AuthController;
import com.example.scm.auth.service.AuthService;
import com.example.scm.auth.vo.LoginResponse;
import com.example.scm.auth.vo.UserProfileResponse;
import com.example.scm.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldHandleAuthEndpoints() throws Exception {
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken("jwt-token");
        loginResponse.setTokenType("Bearer");
        loginResponse.setTenantId(1L);
        loginResponse.setUserId(10001L);
        loginResponse.setUsername("admin");
        loginResponse.setRoles(List.of("ROLE_ADMIN"));
        loginResponse.setExpiresAt(123456L);

        UserProfileResponse profileResponse = new UserProfileResponse();
        profileResponse.setTenantId(1L);
        profileResponse.setUserId(10001L);
        profileResponse.setUsername("admin");
        profileResponse.setRoles(List.of("ROLE_ADMIN"));

        when(authService.login(any())).thenReturn(loginResponse);
        when(authService.refresh("Bearer jwt-token")).thenReturn(loginResponse);
        when(authService.currentUser("Bearer jwt-token")).thenReturn(profileResponse);
        doNothing().when(authService).logout("Bearer jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_ADMIN"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
