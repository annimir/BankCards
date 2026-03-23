package com.example.bankcards.health;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Кастомный HealthIndicator — виден на /actuator/health.
 * Показывает статистику карт и состояние системы, а не просто "UP/DOWN".
 * Позволяет operations-команде видеть метрики прямо из health endpoint'а.
 */
@Slf4j
@Component("cardSystem")
@RequiredArgsConstructor
public class CardSystemHealthIndicator implements HealthIndicator {

    private final CardRepository cardRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Health health() {
        try {
            long totalCards  = cardRepository.count();
            long activeCards = cardRepository.countExpirable(LocalDate.now().plusYears(100), CardStatus.ACTIVE);
            long expiredCards = cardRepository.countExpirable(LocalDate.now(), CardStatus.ACTIVE);
            long staleTokens = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());

            return Health.up()
                    .withDetail("cards.total",   totalCards)
                    .withDetail("cards.active",  activeCards)
                    .withDetail("cards.expired", expiredCards)
                    .withDetail("tokens.stale",  staleTokens)
                    .build();

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}