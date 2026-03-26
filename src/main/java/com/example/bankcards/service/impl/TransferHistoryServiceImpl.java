package com.example.bankcards.service.impl;

import com.example.bankcards.dto.TransferHistoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Контракт сервиса истории переводов.
 *
 * <p>Предоставляет только операции чтения — записи создаются
 * асинхронно через {@link com.example.bankcards.event.TransferEventListener}.
 */
public interface TransferHistoryServiceImpl {
    Page<TransferHistoryDTO> getMyHistory(Long userId, Pageable pageable);
    Page<TransferHistoryDTO> getCardHistory(Long cardId, Long userId, Pageable pageable);
}