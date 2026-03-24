package com.example.bankcards.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Аннотация валидации номера банковской карты по алгоритму Луна.
 *
 * <p>Применяется к полям типа {@code String}. Принимает номера с пробелами и дефисами.
 * Логика проверки реализована в {@link com.example.bankcards.validation.CardNumberValidator}.
 *
 * <pre>{@code
 * @ValidCardNumber
 * private String cardNumber;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCardNumber {
    String message() default "Invalid card number (Luhn check failed)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}