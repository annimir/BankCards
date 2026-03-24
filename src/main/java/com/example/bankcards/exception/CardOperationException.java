package com.example.bankcards.exception;

/**
 * Исключение для недопустимых операций с картой.
 *
 * <p>Выбрасывается когда операция логически невозможна: блокировка истёкшей карты,
 * активация истёкшей карты, перевод с заблокированной карты и т.д.
 * Возвращает клиенту HTTP 400 Bad Request.
 */
public class CardOperationException extends RuntimeException {
    public CardOperationException(String message) {
        super(message);
    }
}
