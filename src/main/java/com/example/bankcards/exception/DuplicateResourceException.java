package com.example.bankcards.exception;

/**
 * Исключение при попытке создать дублирующийся ресурс.
 *
 * <p>Выбрасывается при регистрации с уже занятым username или email.
 * Возвращает клиенту HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
