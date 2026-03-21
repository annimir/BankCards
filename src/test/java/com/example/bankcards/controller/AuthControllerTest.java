package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.RefreshTokenService;
import com.example.bankcards.service.UserDetailsServiceImpl;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController integration tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;
    @MockBean private RefreshTokenService refreshTokenService;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private JwtUtil jwtUtil;

    private static final User STUB_USER = User.builder()
            .id(1L).username("testuser").email("test@test.com")
            .password("encoded").role(Role.USER).enabled(true).build();

    private static final RefreshToken STUB_REFRESH = RefreshToken.builder()
            .token("refresh-uuid-stub").user(STUB_USER)
            .expiresAt(LocalDateTime.now().plusDays(7)).revoked(false).build();

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register - should return 201 with both tokens")
    void register_ShouldReturn201() throws Exception {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");
        request.setPassword("Password1!");

        when(authService.register(any())).thenReturn(
                new AuthDTO.AuthResponse("access-token-123", "refresh-uuid-stub", "testuser", "USER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-uuid-stub"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 on blank username")
    void register_ShouldReturn400WhenBlankUsername() throws Exception {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("");
        request.setEmail("test@test.com");
        request.setPassword("Password1!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 on invalid email")
    void register_ShouldReturn400WhenInvalidEmail() throws Exception {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("not-an-email");
        request.setPassword("Password1!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login - should return 200 with both tokens")
    void login_ShouldReturn200() throws Exception {
        AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
        request.setUsername("testuser");
        request.setPassword("Password1!");

        when(authService.login(any())).thenReturn(
                new AuthDTO.AuthResponse("access-token-123", "refresh-uuid-stub", "testuser", "USER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-uuid-stub"));
    }

    @Test
    @DisplayName("POST /api/auth/login - should return 400 on blank password")
    void login_ShouldReturn400WhenBlankPassword() throws Exception {
        AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
        request.setUsername("testuser");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    // ─── refresh ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/refresh - should return 200 with new token pair")
    void refresh_ShouldReturn200() throws Exception {
        AuthDTO.RefreshRequest request = new AuthDTO.RefreshRequest();
        request.setRefreshToken("refresh-uuid-stub");

        when(authService.refresh(any())).thenReturn(
                new AuthDTO.RefreshResponse("new-access-token", "new-refresh-uuid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-uuid"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - should return 400 on blank refresh token")
    void refresh_ShouldReturn400WhenBlankToken() throws Exception {
        AuthDTO.RefreshRequest request = new AuthDTO.RefreshRequest();
        request.setRefreshToken("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.refreshToken").exists());
    }
}