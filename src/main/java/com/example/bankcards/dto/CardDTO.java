package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO-объекты для операций с картами.
 *
 * <p>Содержит вложенные классы:
 * <ul>
 *   <li>{@code CreateCardRequest} — параметры создания карты (владелец, срок, начальный баланс)</li>
 *   <li>{@code CardResponse} — полное представление карты в ответе API</li>
 *   <li>{@code TransferRequest} — параметры перевода (откуда, куда, сумма)</li>
 * </ul>
 * Номер карты возвращается только в маскированном виде ({@code **** **** **** 1234}).
 */
public class CardDTO {

    @Data
    @Builder
    public static class CardResponse {
        private Long id;
        private String cardNumberMasked;
        private Long ownerId;
        private String ownerUsername;
        private LocalDate expiryDate;
        private CardStatus status;
        private BigDecimal balance;
        private LocalDateTime createdAt;
    }

    @Data
    public static class CreateCardRequest {
        @NotNull(message = "Owner ID is required")
        private Long ownerId;

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        private LocalDate expiryDate;

        @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
        private BigDecimal initialBalance = BigDecimal.ZERO;
    }

    @Data
    public static class TransferRequest {
        @NotNull(message = "Source card ID is required")
        private Long fromCardId;

        @NotNull(message = "Destination card ID is required")
        private Long toCardId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
        private BigDecimal amount;
    }

    @Data
    public static class BlockRequest {
        private String reason;
    }
}
