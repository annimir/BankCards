package com.example.bankcards.service.impl;

import com.example.bankcards.dto.AuthDTO;

/**
 * Контракт сервиса аутентификации.
 *
 * <p>Определяет полный цикл работы с токенами: регистрация, логин,
 * ротация refresh токена и logout со всех устройств.
 */
public interface AuthServiceImpl {
    AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request);
    AuthDTO.AuthResponse login(AuthDTO.LoginRequest request);
    AuthDTO.RefreshResponse refresh(AuthDTO.RefreshRequest request);
    void logout(String refreshToken);
}
