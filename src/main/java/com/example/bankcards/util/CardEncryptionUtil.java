package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class CardEncryptionUtil {

    private static final String ALGORITHM = "AES";

    @Value("${card.encryption.secret}")
    private String secret;

    public String encrypt(String cardNumber) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt card number", e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedCardNumber);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt card number", e);
        }
    }

    /**
     * Returns masked format: **** **** **** 1234
     */
    public String mask(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s", "");
        if (digits.length() != 16) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }
        return "**** **** **** " + digits.substring(12);
    }

    /**
     * Generates a random 16-digit card number
     */
    public String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }
}