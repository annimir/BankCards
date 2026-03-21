package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController integration tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private com.example.bankcards.util.JwtUtil jwtUtil;
    @MockBean private com.example.bankcards.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/auth/register - should return 201 with token")
    void register_ShouldReturn201() throws Exception {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");
        request.setPassword("Password1!");

        AuthDTO.AuthResponse response = new AuthDTO.AuthResponse("token123", "testuser", "USER");
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 on blank username")
    void register_ShouldReturn400WhenInvalidInput() throws Exception {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("");  // blank
        request.setEmail("test@test.com");
        request.setPassword("Password1!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - should return 200 with token")
    void login_ShouldReturn200() throws Exception {
        AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
        request.setUsername("testuser");
        request.setPassword("Password1!");

        AuthDTO.AuthResponse response = new AuthDTO.AuthResponse("token123", "testuser", "USER");
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));
    }
}