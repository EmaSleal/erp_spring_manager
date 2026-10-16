package api.astro.whats_orders_manager.config.crypto;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * T-021 — Cifrado de credenciales en reposo con AES-256-GCM (credenciales de Hacienda y, desde T-022, la
 * contraseña SMTP de configuracion_email).
 *
 * <p>Formato almacenado: {@code "enc:v1:" + base64(iv(12 bytes) || ciphertext + tag(128 bits))}, con un IV
 * aleatorio por valor. La clave viene de {@code HACIENDA_CRYPTO_KEY} (propiedad {@code hacienda.crypto.key}):
 * Base64 de exactamente 32 bytes. Sin clave, el cifrador queda "no configurado": lee texto plano heredado pero
 * no cifra ni descifra. Ningún mensaje incluye la clave ni los valores.
 */
public final class CredencialCipher {

    public static final String PREFIX = "enc:v1:";
    public static final String KEY_ENV_VAR = "HACIENDA_CRYPTO_KEY";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    /**
     * @param base64Key clave en Base64 (32 bytes decodificados); null o vacío = sin clave
     * @throws IllegalArgumentException si la clave no es Base64 válido o no mide 32 bytes
     */
    public CredencialCipher(String base64Key) {
        this.key = base64Key == null || base64Key.isBlank() ? null : new SecretKeySpec(decodeKey(base64Key), "AES");
    }

    /**
     * Valida el formato de la clave sin conservarla.
     *
     * @throws IllegalArgumentException con un mensaje que nombra la variable, nunca el valor
     */
    public static void validateKey(String base64Key) {
        decodeKey(base64Key);
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        requireKey("cifrar");
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar la credencial", e);
        }
    }

    /**
     * Descifra un valor con prefijo; un valor sin prefijo es texto plano heredado y se devuelve tal cual.
     *
     * @throws IllegalStateException si el valor está cifrado y no hay clave, o si está alterado/corrupto
     */
    public String decrypt(String stored) {
        if (stored == null || !isEncrypted(stored)) {
            return stored;
        }
        requireKey("descifrar");
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Credencial cifrada corrupta: el contenido no es Base64 válido");
        }
        if (payload.length < IV_LENGTH_BYTES + TAG_LENGTH_BITS / 8) {
            throw new IllegalStateException("Credencial cifrada corrupta: contenido demasiado corto");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, payload, 0, IV_LENGTH_BYTES));
            byte[] plain = cipher.doFinal(payload, IV_LENGTH_BYTES, payload.length - IV_LENGTH_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new IllegalStateException("Credencial cifrada alterada o cifrada con otra clave ("
                    + KEY_ENV_VAR + ")");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar la credencial", e);
        }
    }

    private void requireKey(String operacion) {
        if (key == null) {
            throw new IllegalStateException("No se puede " + operacion + " credenciales (Hacienda, SMTP): falta "
                    + "la variable de entorno " + KEY_ENV_VAR + ". Nunca se guardan en texto plano.");
        }
    }

    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException(KEY_ENV_VAR + " está vacía.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(KEY_ENV_VAR + " no es Base64 válido.");
        }
        if (decoded.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(KEY_ENV_VAR + " debe decodificar a exactamente " + KEY_LENGTH_BYTES
                    + " bytes (AES-256) y decodifica a " + decoded.length + ".");
        }
        return decoded;
    }
}
