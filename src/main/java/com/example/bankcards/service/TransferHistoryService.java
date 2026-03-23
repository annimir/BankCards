package com.example.bankcards.service;

import com.example.bankcards.dto.TransferHistoryDTO;
import com.example.bankcards.entity.TransferHistory;
import com.example.bankcards.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferHistoryService {

    private final TransferHistoryRepository transferHistoryRepository;

    @Transactional(readOnly = true)
    public Page<TransferHistoryDTO> getMyHistory(Long userId, Pageable pageable) {
        log.debug("Fetching transfer history for userId={}", userId);
        return transferHistoryRepository
                .findByOwnerIdOrderByOccurredAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TransferHistoryDTO> getCardHistory(Long cardId, Pageable pageable) {
        log.debug("Fetching transfer history for cardId={}", cardId);
        return transferHistoryRepository
                .findByFromCardIdOrToCardIdOrderByOccurredAtDesc(cardId, cardId, pageable)
                .map(this::toDto);
    }

    private TransferHistoryDTO toDto(TransferHistory h) {
        return TransferHistoryDTO.builder()
                .id(h.getId())
                .fromCardId(h.getFromCard().getId())
                .fromCardMasked(h.getFromCard().getCardNumberMasked())
                .toCardId(h.getToCard().getId())
                .toCardMasked(h.getToCard().getCardNumberMasked())
                .amount(h.getAmount())
                .balanceAfter(h.getBalanceAfter())
                .occurredAt(h.getOccurredAt())
                .build();
    }
}