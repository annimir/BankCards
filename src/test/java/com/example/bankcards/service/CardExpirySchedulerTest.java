package com.example.bankcards.service;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardExpiryScheduler unit tests")
class CardExpirySchedulerTest {

    @Mock private CardRepository cardRepository;
    @InjectMocks private CardExpiryScheduler scheduler;

    @Test
    @DisplayName("expireOutdatedCards: should call expireCards when expirable cards exist")
    void expireOutdatedCards_WithExpirableCards() {
        when(cardRepository.countExpirable(any(), eq(CardStatus.ACTIVE))).thenReturn(3L);
        when(cardRepository.expireCards(any(), eq(CardStatus.ACTIVE), eq(CardStatus.EXPIRED))).thenReturn(3);

        scheduler.expireOutdatedCards();

        verify(cardRepository).expireCards(LocalDate.now(), CardStatus.ACTIVE, CardStatus.EXPIRED);
    }

    @Test
    @DisplayName("expireOutdatedCards: should skip update when no expirable cards")
    void expireOutdatedCards_NothingToExpire() {
        when(cardRepository.countExpirable(any(), eq(CardStatus.ACTIVE))).thenReturn(0L);

        scheduler.expireOutdatedCards();

        verify(cardRepository, never()).expireCards(any(), any(), any());
    }

    @Test
    @DisplayName("expireOutdatedBlockedCards: should expire blocked cards too")
    void expireOutdatedBlockedCards() {
        when(cardRepository.expireCards(any(), eq(CardStatus.BLOCKED), eq(CardStatus.EXPIRED))).thenReturn(2);

        scheduler.expireOutdatedBlockedCards();

        verify(cardRepository).expireCards(LocalDate.now(), CardStatus.BLOCKED, CardStatus.EXPIRED);
    }
}