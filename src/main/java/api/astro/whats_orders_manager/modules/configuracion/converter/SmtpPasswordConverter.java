package api.astro.whats_orders_manager.modules.configuracion.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter that transparently encrypts smtpPassword at rest using AES-256-GCM.
 *
 * Stored format: {enc}<Base64(IV[12] || Ciphertext || GCM-Tag[16])>
 * Legacy plain-text values (no {enc} prefix) are returned as-is so existing rows
 * continue to work until the operator re-saves the configuration.
 *
 * Key setup: set SMTP_ENCRYPTION_KEY env var to a Base64-encoded 32-byte (256-bit) key.
 * Generate one with: openssl rand -base64 32
 * If the key is absent, values are stored and returned unencrypted (opt-in encryption).
 */
@Component
@Converter(autoApply = false)
public class SmtpPasswordConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "{enc}";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    @Value("${smtp.encryption.key:}")
    private String encryptionKeyBase64;

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null || plaintext.isEmpty() || encryptionKeyBase64.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            byte[] payload = new byte[IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, payload, IV_LENGTH_BYTES, ciphertext.length);

            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt SMTP password", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null || !dbValue.startsWith(PREFIX) || encryptionKeyBase64.isEmpty()) {
            return dbValue;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(dbValue.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(payload, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt SMTP password", e);
        }
    }

    private SecretKeySpec buildKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "SMTP_ENCRYPTION_KEY must decode to exactly 32 bytes (256-bit AES key). " +
                "Generate one with: openssl rand -base64 32"
            );
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
