package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Random;

/**
 * Утилита шифрования и генерации номеров банковских карт.
 *
 * <p>Шифрование AES с ключом из {@code card.encryption.secret} (32 байта).
 * В БД хранится Base64-закодированный зашифрованный номер, в API-ответах — только маска.
 *
 * <p>Генерация номеров: первые 15 цифр случайны (BIN начинается с 4 — Visa),
 * 16-я вычисляется как контрольная сумма по алгоритму Луна — все генерируемые
 * номера проходят валидацию {@link com.example.bankcards.validation.CardNumberValidator}.
 */
@Component
public class CardEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final Random RANDOM = new Random();

    @Value("${card.encryption.secret}")
    private String secret;

    public String encrypt(String cardNumber) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(cardNumber.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt card number", e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedCardNumber)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt card number", e);
        }
    }

    /**
     * Маскирует номер карты: **** **** **** 1234
     */
    public String mask(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s", "");
        if (digits.length() != 16) throw new IllegalArgumentException("Card number must be 16 digits");
        return "**** **** **** " + digits.substring(12);
    }

    /**
     * Генерирует случайный 16-значный номер карты, проходящий алгоритм Луна.
     * Первые 15 цифр — случайные, 16-я — контрольная.
     */
    public String generateCardNumber() {
        int[] digits = new int[16];
        // BIN (Bank Identification Number) — начинаем с 4 (Visa-like)
        digits[0] = 4;
        for (int i = 1; i < 15; i++) {
            digits[i] = RANDOM.nextInt(10);
        }
        digits[15] = calculateLuhnCheckDigit(digits);
        StringBuilder sb = new StringBuilder();
        for (int d : digits) sb.append(d);
        return sb.toString();
    }

    /**
     * Вычисляет контрольную цифру по алгоритму Луна.
     */
    private int calculateLuhnCheckDigit(int[] digits) {
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            int d = digits[i];
            if (i % 2 == 0) {         // чётные позиции (0-based) удваиваем
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
        }
        return (10 - (sum % 10)) % 10;
    }
}