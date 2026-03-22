package com.example.bankcards.validation;

import com.example.bankcards.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardNumberValidator (Luhn) unit tests")
class CardNumberValidatorTest {

    private CardNumberValidator validator;
    private CardEncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        validator = new CardNumberValidator();
        encryptionUtil = new CardEncryptionUtil();
        ReflectionTestUtils.setField(encryptionUtil, "secret", "MySecretKey12345MySecretKey12345");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4532015112830366",  // валидный Visa
            "5425233430109903",  // валидный Mastercard
            "4111111111111111",  // классический тестовый номер
            "4532 0151 1283 0366" // с пробелами
    })
    @DisplayName("isValid: returns true for valid Luhn numbers")
    void isValid_ValidNumbers(String cardNumber) {
        assertThat(validator.isValid(cardNumber, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1234567890123456",  // неверная контрольная сумма
            "0000000000000000",  // нули — не проходит Luhn
            "123",               // слишком короткий
            "abcdefghijklmnop", // не цифры
            "",                  // пустой
    })
    @DisplayName("isValid: returns false for invalid numbers")
    void isValid_InvalidNumbers(String cardNumber) {
        assertThat(validator.isValid(cardNumber, null)).isFalse();
    }

    @Test
    @DisplayName("isValid: returns false for null")
    void isValid_Null() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    @DisplayName("generateCardNumber: all generated numbers pass Luhn check")
    void generatedNumbers_PassLuhnCheck() {
        for (int i = 0; i < 100; i++) {
            String cardNumber = encryptionUtil.generateCardNumber();
            assertThat(validator.isValid(cardNumber, null))
                    .as("Generated number %s should be valid", cardNumber)
                    .isTrue();
        }
    }
}