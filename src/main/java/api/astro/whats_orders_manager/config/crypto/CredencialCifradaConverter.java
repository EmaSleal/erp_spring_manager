package api.astro.whats_orders_manager.config.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;

/**
 * T-021 — Cifra credenciales al escribir y las descifra al leer (ver {@link CredencialCipher}).
 *
 * <p>Hibernate crea el converter a través del SpringBeanContainer de Spring Boot, que resuelve el
 * {@code @Value} del constructor también en los slices de prueba (@DataJpaTest). Lectura: valor con prefijo
 * {@code enc:v1:} → se descifra (alterado → excepción); sin prefijo → texto plano heredado, tal cual.
 * Escritura: siempre cifra; sin clave lanza IllegalStateException (nunca guarda texto plano). null → null.
 */
@Converter
public class CredencialCifradaConverter implements AttributeConverter<String, String> {

    private final CredencialCipher cipher;

    public CredencialCifradaConverter(@Value("${hacienda.crypto.key:}") String base64Key) {
        this.cipher = new CredencialCipher(base64Key);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cipher.decrypt(dbData);
    }
}
