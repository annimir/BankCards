package com.example.bankcards.exception;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений.
 * Использует ProblemDetail (RFC 7807 / Spring 6) вместо самописного ErrorResponse.
 * Это стандарт для REST API — клиенты любого языка понимают этот формат.
 *
 * Формат ответа:
 * {
 *   "type": "https://bankcards.example.com/errors/not-found",
 *   "title": "Not Found",
 *   "status": 404,
 *   "detail": "Card not found with id: 5",
 *   "instance": "/api/user/cards/5"   (добавляется Spring автоматически)
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://bankcards.example.com/errors/";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "not-found", ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient funds: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient-funds", ex.getMessage());
    }

    @ExceptionHandler(CardOperationException.class)
    public ProblemDetail handleCardOperation(CardOperationException ex) {
        log.warn("Card operation error: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "card-operation-error", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "duplicate-resource", ex.getMessage());
    }

    /**
     * Optimistic locking — параллельное изменение одной карты двумя потоками.
     * Клиент получает 409 и должен повторить запрос.
     */
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleOptimisticLock(Exception ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "concurrent-modification",
                "The resource was modified by another request. Please retry.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: bad credentials");
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid username or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "access-denied", "Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        log.warn("Validation failed: {}", errors);

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation-error", "Validation failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error");
    }

    private ProblemDetail problem(HttpStatus status, String errorCode, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(ERROR_BASE_URI + errorCode));
        pd.setTitle(status.getReasonPhrase());
        return pd;
    }
}