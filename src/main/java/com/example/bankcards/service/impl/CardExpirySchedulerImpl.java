package com.example.bankcards.service.impl;

/**
 * Контракт аутентификации и управления токенами.
 *
 * <p>Реализует полный цикл работы с токенами</p>
 */
public interface CardExpirySchedulerImpl {

    public void expireOutdatedCards();
    public void expireOutdatedBlockedCards();
}
