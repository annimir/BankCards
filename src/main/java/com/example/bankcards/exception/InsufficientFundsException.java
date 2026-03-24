package com.example.bankcards.exception;

/**
 * Исключение при недостатке средств для перевода.
 *
 * <p>Выбрасывается в {@code CardService#transfer} когда баланс карты-источника
 * меньше запрошенной суммы. Возвращает клиенту HTTP 422 Unprocessable Entity.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
