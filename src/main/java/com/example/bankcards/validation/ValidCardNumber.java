package com.example.bankcards.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Проверяет что строка является валидным номером карты по алгоритму Луна.
 * Использование: @ValidCardNumber на поле String.
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