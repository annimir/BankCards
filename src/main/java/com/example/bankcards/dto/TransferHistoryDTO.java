package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransferHistoryDTO {
    private Long id;
    private Long fromCardId;
    private String fromCardMasked;
    private Long toCardId;
    private String toCardMasked;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime occurredAt;
}