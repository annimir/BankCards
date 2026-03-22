package com.example.bankcards.integration;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end тесты аутентификации с реальным PostgreSQL.
 * Проверяет полный цикл: регистрация → логин → refresh → logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Auth integration tests (Testcontainers + PostgreSQL)")
class AuthIntegrationTest extends PostgreSQLContainerBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("register → login → refresh: full token lifecycle")
    void fullAuthLifecycle() throws Exception {
        String username = "e2euser_" + System.nanoTime();

        // 1. Регистрация
        AuthDTO.RegisterRequest registerRequest = new AuthDTO.RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@test.com");
        registerRequest.setPassword("Password1!");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        AuthDTO.AuthResponse authResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(), AuthDTO.AuthResponse.class);

        assertThat(userRepository.findByUsername(username)).isPresent();

        // 2. Логин
        AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("Password1!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        AuthDTO.AuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthDTO.AuthResponse.class);

        // 3. Refresh — используем refresh token из логина
        AuthDTO.RefreshRequest refreshRequest = new AuthDTO.RefreshRequest();
        refreshRequest.setRefreshToken(loginResponse.getRefreshToken());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthDTO.RefreshResponse refreshResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), AuthDTO.RefreshResponse.class);

        // Новый refresh token отличается от старого (rotation)
        assertThat(refreshResponse.getRefreshToken())
                .isNotEqualTo(loginResponse.getRefreshToken());

        // 4. Старый refresh token больше не работает (rotation — он был revoked)
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest))) // старый токен
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register: duplicate username returns 409")
    void register_DuplicateUsername_Returns409() throws Exception {
        String username = "dupuser_" + System.nanoTime();

        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@test.com");
        request.setPassword("Password1!");

        // Первая регистрация — успешно
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Вторая — конфликт
        request.setEmail("other_" + username + "@test.com"); // другой email, тот же username
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login: wrong password returns 401")
    void login_WrongPassword_Returns401() throws Exception {
        // Сначала регистрируем пользователя
        String username = "logintest_" + System.nanoTime();
        AuthDTO.RegisterRequest register = new AuthDTO.RegisterRequest();
        register.setUsername(username);
        register.setEmail(username + "@test.com");
        register.setPassword("CorrectPassword1!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // Логин с неверным паролем
        AuthDTO.LoginRequest login = new AuthDTO.LoginRequest();
        login.setUsername(username);
        login.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}