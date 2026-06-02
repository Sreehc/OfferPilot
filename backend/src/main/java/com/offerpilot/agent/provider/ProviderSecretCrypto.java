package com.offerpilot.agent.provider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProviderSecretCrypto {

    private final String jwtSecret;

    public ProviderSecretCrypto(@Value("${offerpilot.security.jwt-secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getAesKey());
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("encrypt provider secret failed", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getAesKey());
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt provider secret failed", e);
        }
    }

    public String mask(String encrypted) {
        String plainText = decrypt(encrypted);
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        int visible = Math.min(4, plainText.length());
        if (plainText.length() <= visible) {
            return "*".repeat(plainText.length());
        }
        return "*".repeat(Math.max(0, plainText.length() - visible)) + plainText.substring(plainText.length() - visible);
    }

    private SecretKeySpec getAesKey() {
        byte[] source = jwtSecret.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = new byte[16];
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = i < source.length ? source[i] : (byte) '0';
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
