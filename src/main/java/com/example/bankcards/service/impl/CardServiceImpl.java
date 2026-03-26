package com.example.bankcards.service.impl;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Контракт сервиса банковских карт.
 *
 * <p>Разделён на две зоны ответственности:
 * <ul>
 *   <li>Admin-операции — создание, активация, блокировка, удаление</li>
 *   <li>User-операции — просмотр своих карт, запрос блокировки, переводы</li>
 * </ul>
 */
public interface CardServiceImpl {

    // Admin
    CardDTO.CardResponse createCard(CardDTO.CreateCardRequest request);
    Page<CardDTO.CardResponse> getAllCards(CardStatus status, Long ownerId, Pageable pageable);
    CardDTO.CardResponse getCardById(Long id);
    CardDTO.CardResponse activateCard(Long id);
    CardDTO.CardResponse blockCard(Long id);
    void deleteCard(Long id);

    // User
    Page<CardDTO.CardResponse> getMyCards(Long userId, CardStatus status, String maskedNumber, Pageable pageable);
    CardDTO.CardResponse getMyCard(Long cardId, Long userId);
    CardDTO.CardResponse requestBlock(Long cardId, Long userId);
    void transfer(Long userId, CardDTO.TransferRequest request);
}