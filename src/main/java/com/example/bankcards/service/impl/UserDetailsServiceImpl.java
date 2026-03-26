package com.example.bankcards.service.impl;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Контракт {@code UserDetailsService} для Spring Security.
 *
 * <p>Загружает пользователя по username из БД для аутентификации.
 * {@link com.example.bankcards.entity.User} реализует {@code UserDetails} напрямую,
 * поэтому адаптер не нужен — возвращается сама сущность.
 */
public interface UserDetailsServiceImpl {
    public UserDetails loadUserByUsername(String username);
}
