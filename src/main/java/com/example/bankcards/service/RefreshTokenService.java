package com.example.bankcards.service;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    /**
     * Creates a new refresh token for the given user.
     * Any previous tokens are NOT revoked here — one user can have
     * multiple sessions (e.g. mobile + desktop).
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .revoked(false)
                .build();
        RefreshToken saved = refreshTokenRepository.save(token);
        log.debug("Refresh token created for userId={}", user.getId());
        return saved;
    }

    /**
     * Validates the token string and returns the associated entity.
     * Throws if token is not found, revoked, or expired.
     */
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found");
                    return new CardOperationException("Invalid refresh token");
                });

        if (!refreshToken.isValid()) {
            log.warn("Refresh token is invalid: revoked={}, expired={}",
                    refreshToken.isRevoked(), refreshToken.isExpired());
            throw new CardOperationException("Refresh token is expired or revoked. Please login again.");
        }

        return refreshToken;
    }

    /**
     * Revokes all active refresh tokens for a user (logout from all sessions).
     */
    @Transactional
    public void revokeAll(Long userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Revoked {} refresh token(s) for userId={}", count, userId);
    }

    /**
     * Cleanup job: removes stale tokens every day at 03:00 AM.
     * Keeps the table lean — no unbounded growth.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now();
        int deleted = refreshTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Refresh token cleanup: {} stale token(s) deleted.", deleted);
    }
}