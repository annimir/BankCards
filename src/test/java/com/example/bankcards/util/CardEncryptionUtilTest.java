package com.example.bankcards.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CardEncryptionUtil unit tests")
class CardEncryptionUtilTest {

    private CardEncryptionUtil util;

    @BeforeEach
    void setUp() {
        util = new CardEncryptionUtil();
        ReflectionTestUtils.setField(util, "secret", "MySecretKey12345MySecretKey12345");
    }

    @Test
    @DisplayName("encrypt/decrypt: should round-trip correctly")
    void encryptDecrypt_RoundTrip() {
        String original = "1234567890123456";
        String encrypted = util.encrypt(original);
        String decrypted = util.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(original);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("mask: should mask card number correctly")
    void mask_ShouldReturnMaskedFormat() {
        String masked = util.mask("1234567890123456");
        assertThat(masked).isEqualTo("**** **** **** 3456");
    }

    @Test
    @DisplayName("mask: should throw exception for non-16-digit number")
    void mask_InvalidLength_ShouldThrow() {
        assertThatThrownBy(() -> util.mask("123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16 digits");
    }

    @Test
    @DisplayName("generateCardNumber: should return 16 digits")
    void generateCardNumber_ShouldReturn16Digits() {
        String cardNumber = util.generateCardNumber();
        assertThat(cardNumber).hasSize(16);
        assertThat(cardNumber).matches("\\d{16}");
    }
}