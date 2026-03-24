package com.example.bankcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа приложения BankCards.
 *
 * <p>{@code @EnableScheduling} активирует поддержку {@code @Scheduled} для фоновых задач:
 * истечение карт в {@code CardExpiryScheduler} и очистка токенов в {@code RefreshTokenService}.
 *
 * <p>{@code @EnableAsync} позволяет {@code @Async}-методам выполняться в отдельном потоке —
 * используется в {@code TransferEventListener} чтобы запись истории не блокировала HTTP-ответ.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}