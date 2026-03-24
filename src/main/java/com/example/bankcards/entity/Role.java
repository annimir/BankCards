package com.example.bankcards.entity;

/**
 * Роль пользователя в системе.
 *
 * <ul>
 *   <li>{@code USER} — обычный пользователь: видит и управляет только своими картами</li>
 *   <li>{@code ADMIN} — администратор: полный доступ ко всем картам и пользователям</li>
 * </ul>
 * Spring Security читает роль через {@code ROLE_} префикс: {@code ROLE_USER}, {@code ROLE_ADMIN}.
 */
public enum Role {
    USER,
    ADMIN
}
