package com.example.bankcards.event;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.TransferHistory;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferHistoryRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Слушатель доменных событий карт.
 *
 * <p>{@code @Async} — обработка в отдельном потоке, не блокирует основную транзакцию.
 * Добавление новых реакций (push, email, SMS) требует только нового метода здесь —
 * бизнес-логика сервисов остаётся без изменений (принцип Open/Closed).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventListener {

    private final TransferHistoryRepository transferHistoryRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Async
    @EventListener
    @Transactional
    public void onTransferCompleted(TransferCompletedEvent event) {
        try {
            Card fromCard = cardRepository.findById(event.getFromCardId()).orElse(null);
            Card toCard   = cardRepository.findById(event.getToCardId()).orElse(null);
            User owner    = userRepository.findById(event.getOwnerId()).orElse(null);

            if (fromCard == null || toCard == null || owner == null) {
                log.warn("TransferEventListener: could not find entities for event, skipping history record");
                return;
            }

            TransferHistory history = TransferHistory.builder()
                    .fromCard(fromCard)
                    .toCard(toCard)
                    .owner(owner)
                    .amount(event.getAmount())
                    .balanceAfter(event.getBalanceAfter())
                    .occurredAt(event.getOccurredAt())
                    .build();

            transferHistoryRepository.save(history);
            log.info("Transfer history saved: from={}, to={}, amount={}",
                    event.getFromCardId(), event.getToCardId(), event.getAmount());

        } catch (Exception e) {
            // Ошибка записи истории не должна влиять на основной поток
            log.error("Failed to save transfer history for event: {}", e.getMessage(), e);
        }
    }
}