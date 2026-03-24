package com.example.bankcards.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Событие блокировки банковской карты.
 *
 * <p>Публикуется после успешной блокировки — пользователем через запрос
 * или администратором напрямую. Слушатели могут реагировать на событие
 * независимо (уведомления, аудит) без изменения бизнес-логики сервиса.
 */
@Getter
public class TransferCompletedEvent extends ApplicationEvent {

    private final Long fromCardId;
    private final Long toCardId;
    private final Long ownerId;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final LocalDateTime occurredAt;

    public TransferCompletedEvent(Object source,
                                  Long fromCardId,
                                  Long toCardId,
                                  Long ownerId,
                                  BigDecimal amount,
                                  BigDecimal balanceAfter) {
        super(source);
        this.fromCardId  = fromCardId;
        this.toCardId    = toCardId;
        this.ownerId     = ownerId;
        this.amount      = amount;
        this.balanceAfter = balanceAfter;
        this.occurredAt  = LocalDateTime.now();
    }
}