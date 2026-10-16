package api.astro.whats_orders_manager.config.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * T-021 — Expone el {@link CredencialCipher} construido desde {@code hacienda.crypto.key}. El formato de la
 * clave ya se validó al arrancar (RequiredConfigurationEnvironmentPostProcessor).
 */
@Configuration
public class CredencialCipherConfig {

    @Bean
    public CredencialCipher credencialCipher(@Value("${hacienda.crypto.key:}") String base64Key) {
        return new CredencialCipher(base64Key);
    }
}
