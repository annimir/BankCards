package com.example.bankcards.util;

import com.example.bankcards.validation.CardNumberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CardEncryptionUtil unit tests")
class CardEncryptionUtilTest {

    private CardEncryptionUtil util;
    private CardNumberValidator luhnValidator;

    @BeforeEach
    void setUp() {
        util = new CardEncryptionUtil();
        ReflectionTestUtils.setField(util, "secret", "MySecretKey12345MySecretKey12345");
        luhnValidator = new CardNumberValidator();
    }

    @Test
    @DisplayName("encrypt/decrypt: should round-trip correctly")
    void encryptDecrypt_RoundTrip() {
        String original = "4532015112830366";
        String encrypted = util.encrypt(original);
        String decrypted = util.decrypt(encrypted);
        assertThat(encrypted).isNotEqualTo(original);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("mask: should mask card number correctly")
    void mask_ShouldReturnMaskedFormat() {
        assertThat(util.mask("1234567890123456")).isEqualTo("**** **** **** 3456");
    }

    @Test
    @DisplayName("mask: should throw for non-16-digit number")
    void mask_InvalidLength_ShouldThrow() {
        assertThatThrownBy(() -> util.mask("123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16 digits");
    }

    @Test
    @DisplayName("generateCardNumber: should return 16 digits")
    void generateCardNumber_ShouldReturn16Digits() {
        String cardNumber = util.generateCardNumber();
        assertThat(cardNumber).hasSize(16).matches("\\d{16}");
    }

    @Test
    @DisplayName("generateCardNumber: generated number passes Luhn check")
    void generateCardNumber_PassesLuhn() {
        for (int i = 0; i < 50; i++) {
            String cardNumber = util.generateCardNumber();
            assertThat(luhnValidator.isValid(cardNumber, null))
                    .as("Card number %s should pass Luhn", cardNumber)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("generateCardNumber: starts with 4 (Visa BIN)")
    void generateCardNumber_StartsWithVisa() {
        String cardNumber = util.generateCardNumber();
        assertThat(cardNumber).startsWith("4");
    }
}