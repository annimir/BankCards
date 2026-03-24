package com.example.bankcards.entity;

/**
 * Статус банковской карты.
 *
 * <ul>
 *   <li>{@code ACTIVE} — карта активна, доступны все операции</li>
 *   <li>{@code BLOCKED} — карта заблокирована, операции запрещены</li>
 *   <li>{@code EXPIRED} — карта истекла, устанавливается автоматически
 *       планировщиком {@code CardExpiryScheduler}</li>
 * </ul>
 */
public enum CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED
}
