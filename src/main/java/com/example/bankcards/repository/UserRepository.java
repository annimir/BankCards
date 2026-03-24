package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link com.example.bankcards.entity.User}.
 *
 * <p>{@code findByUsername} используется {@code UserDetailsServiceImpl}
 * для аутентификации через Spring Security.
 *
 * <p>{@code existsByUsername} и {@code existsByEmail} применяются при регистрации
 * для проверки уникальности без загрузки полной сущности.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
