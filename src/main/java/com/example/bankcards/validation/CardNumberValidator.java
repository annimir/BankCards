package com.example.bankcards.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Реализация алгоритма Луна для валидации номеров банковских карт.
 *
 * <p>Алгоритм (обход справа налево):
 * <ol>
 *   <li>Каждую вторую цифру, начиная со второй справа, умножаем на 2</li>
 *   <li>Если результат больше 9 — вычитаем 9</li>
 *   <li>Сумма всех цифр должна делиться на 10 без остатка</li>
 * </ol>
 *
 * <p>Принимает номера длиной 13–19 цифр, допускает пробелы и дефисы как разделители.
 * Все номера генерируемые {@link com.example.bankcards.util.CardEncryptionUtil#generateCardNumber()}
 * гарантированно проходят эту проверку.
 */
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return false;

        String digits = value.replaceAll("\\s|-", "");
        if (!digits.matches("\\d{13,19}")) return false;

        return luhnCheck(digits);
    }

    private boolean luhnCheck(String digits) {
        int sum = 0;
        boolean doubleDigit = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(digits.charAt(i));
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}