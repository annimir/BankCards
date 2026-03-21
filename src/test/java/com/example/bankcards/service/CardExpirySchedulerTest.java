package com.example.bankcards.service;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardExpiryScheduler unit tests")
class CardExpirySchedulerTest {

    @Mock private CardRepository cardRepository;
    @InjectMocks private CardExpiryScheduler scheduler;

    @Test
    @DisplayName("expireOutdatedCards: should call expireCards when expirable cards exist")
    void expireOutdatedCards_WithExpirableCards() {
        when(cardRepository.countExpirable(any(LocalDate.class), eq(CardStatus.ACTIVE)))
                .thenReturn(3L);
        when(cardRepository.expireCards(any(LocalDate.class), eq(CardStatus.ACTIVE), eq(CardStatus.EXPIRED)))
                .thenReturn(3);

        scheduler.expireOutdatedCards();

        // Capture the LocalDate argument to avoid flakiness at midnight
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(cardRepository).expireCards(
                dateCaptor.capture(),
                eq(CardStatus.ACTIVE),
                eq(CardStatus.EXPIRED));

        assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("expireOutdatedCards: should skip update when no expirable cards")
    void expireOutdatedCards_NothingToExpire() {
        when(cardRepository.countExpirable(any(LocalDate.class), eq(CardStatus.ACTIVE)))
                .thenReturn(0L);

        scheduler.expireOutdatedCards();

        verify(cardRepository, never()).expireCards(any(), any(), any());
    }

    @Test
    @DisplayName("expireOutdatedBlockedCards: should expire blocked cards too")
    void expireOutdatedBlockedCards() {
        when(cardRepository.expireCards(any(LocalDate.class), eq(CardStatus.BLOCKED), eq(CardStatus.EXPIRED)))
                .thenReturn(2);

        scheduler.expireOutdatedBlockedCards();

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(cardRepository).expireCards(
                dateCaptor.capture(),
                eq(CardStatus.BLOCKED),
                eq(CardStatus.EXPIRED));

        assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("expireOutdatedCards: should log and return early when count is zero")
    void expireOutdatedCards_ZeroCount_NoSaveCall() {
        when(cardRepository.countExpirable(any(LocalDate.class), eq(CardStatus.ACTIVE)))
                .thenReturn(0L);

        scheduler.expireOutdatedCards();

        // countExpirable called once, expireCards never called
        verify(cardRepository, times(1)).countExpirable(any(LocalDate.class), eq(CardStatus.ACTIVE));
        verify(cardRepository, never()).expireCards(any(), any(), any());
    }
}