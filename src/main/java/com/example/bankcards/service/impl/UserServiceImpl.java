package com.example.bankcards.service.impl;

import com.example.bankcards.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Контракт сервиса управления пользователями.
 *
 * <p>Разделён на две зоны ответственности:
 * <ul>
 *   <li>Admin-операции — CRUD пользователей, включение/отключение аккаунтов</li>
 *   <li>User-операции — просмотр и редактирование собственного профиля</li>
 * </ul>
 */
public interface UserServiceImpl {
    Page<UserDTO.UserResponse> getAllUsers(Pageable pageable);
    UserDTO.UserResponse getUserById(Long id);
    UserDTO.UserResponse getMyProfile(String username);
    UserDTO.UserResponse createUser(UserDTO.AdminCreateUserRequest request);
    UserDTO.UserResponse updateMyProfile(String username, UserDTO.UpdateUserRequest request);
    void deleteUser(Long id);
    UserDTO.UserResponse toggleUserEnabled(Long id, boolean enabled);
}