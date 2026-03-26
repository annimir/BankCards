package com.example.bankcards.service.impl;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;

/**
 * Контракт сервиса управления refresh токенами.
 *
 * <p>Создание, верификация, отзыв токенов и плановая очистка устаревших записей.
 */
public interface RefreshTokenServiceImpl {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyRefreshToken(String token);
    void revokeAll(Long userId);
}