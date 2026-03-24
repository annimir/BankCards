package com.example.bankcards.repository;

import com.example.bankcards.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Репозиторий для работы с {@link com.example.bankcards.entity.RefreshToken}.
 *
 * <p>{@code revokeAllByUserId} используется при logout — отзывает все активные сессии
 * пользователя одним UPDATE-запросом.
 *
 * <p>{@code deleteExpiredTokens} вызывается планировщиком ежедневно в 03:00
 * для очистки устаревших записей.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Revoke all tokens for a user on logout
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    // Cleanup job: delete expired or revoked tokens older than 30 days
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true OR r.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);
}