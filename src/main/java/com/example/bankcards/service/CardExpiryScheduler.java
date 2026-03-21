package com.example.bankcards.service;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Scheduled job that marks cards as EXPIRED when their expiry date has passed.
 * Runs every day at 01:00 AM server time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpiryScheduler {

    private final CardRepository cardRepository;

    /**
     * Checks ACTIVE cards and marks them EXPIRED if expiryDate < today.
     * Uses a bulk UPDATE query for performance — avoids loading entities into memory.
     * cron = "0 0 1 * * *" => every day at 01:00:00
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireOutdatedCards() {
        LocalDate today = LocalDate.now();
        log.info("Card expiry job started. Checking cards with expiryDate < {}", today);

        long expirableCount = cardRepository.countExpirable(today, CardStatus.ACTIVE);
        if (expirableCount == 0) {
            log.info("Card expiry job: no cards to expire today.");
            return;
        }

        int updated = cardRepository.expireCards(today, CardStatus.ACTIVE, CardStatus.EXPIRED);
        log.info("Card expiry job completed: {} card(s) marked as EXPIRED.", updated);
    }

    /**
     * Also expire BLOCKED cards whose time has passed —
     * a blocked card shouldn't become "unblocked" just because it expired.
     */
    @Scheduled(cron = "0 5 1 * * *")
    @Transactional
    public void expireOutdatedBlockedCards() {
        LocalDate today = LocalDate.now();
        int updated = cardRepository.expireCards(today, CardStatus.BLOCKED, CardStatus.EXPIRED);
        if (updated > 0) {
            log.info("Card expiry job: {} BLOCKED card(s) also marked as EXPIRED.", updated);
        }
    }
}