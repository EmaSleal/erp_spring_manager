package api.astro.whats_orders_manager.config.startup;

import api.astro.whats_orders_manager.config.crypto.CredencialCipher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * T-021 — Falla el arranque con un mensaje claro si falta la configuración sin la cual la aplicación apuntaría a
 * un destino implícito: base de datos y token del webhook ya no tienen valores por defecto en application.yml.
 *
 * <ul>
 *   <li>DB_URL, DB_USERNAME, META_WEBHOOK_VERIFY_TOKEN: deben resolver a un valor no vacío.</li>
 *   <li>DB_PASSWORD: debe estar definida; el valor vacío está permitido.</li>
 *   <li>HACIENDA_CRYPTO_KEY: opcional; si se define, debe ser Base64 de 32 bytes.</li>
 * </ul>
 * Corre después de cargar application.yml/.env (precedencia más baja). Los mensajes nombran variables, nunca
 * valores. Registrado en META-INF/spring.factories.
 */
public class RequiredConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Propiedad → variable de entorno que la alimenta en application.yml, y si admite valor vacío. */
    private record Required(String property, String envVar, boolean mayBeEmpty) {
    }

    private static final List<Required> REQUIRED = List.of(
            new Required("spring.datasource.url", "DB_URL", false),
            new Required("spring.datasource.username", "DB_USERNAME", false),
            new Required("spring.datasource.password", "DB_PASSWORD", true),
            new Required("meta.webhook.verify-token", "META_WEBHOOK_VERIFY_TOKEN", false));

    static final String CRYPTO_KEY_PROPERTY = "hacienda.crypto.key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missing = new ArrayList<>();
        for (Required required : REQUIRED) {
            String value = resolve(environment, required.property());
            if (value == null || (!required.mayBeEmpty() && value.isBlank())) {
                missing.add(required.envVar());
            }
        }
        if (!missing.isEmpty()) {
            throw new StartupConfigurationException(
                    "Faltan variables de entorno obligatorias: " + String.join(", ", missing) + ".",
                    "Defina " + String.join(", ", missing) + " en el entorno o en el archivo .env junto a la "
                            + "aplicación. No hay valores por defecto: la aplicación no se conecta a ningún destino "
                            + "implícito.");
        }
        validateCryptoKey(environment);
    }

    private static void validateCryptoKey(ConfigurableEnvironment environment) {
        String key = resolve(environment, CRYPTO_KEY_PROPERTY);
        if (key == null || key.isBlank()) {
            return; // ausencia permitida aquí; HaciendaCredencialesCifradoRunner decide según haya filas
        }
        try {
            CredencialCipher.validateKey(key);
        } catch (IllegalArgumentException e) {
            throw new StartupConfigurationException(
                    "La clave de cifrado de credenciales de Hacienda es inválida: " + e.getMessage(),
                    "Genere una clave con 'openssl rand -base64 32' y asígnela a " + CredencialCipher.KEY_ENV_VAR
                            + ". Si ya hay credenciales cifradas, use la clave original (respaldada aparte).");
        }
    }

    /** Valor resuelto, o null si la propiedad no existe o su placeholder no se puede resolver. */
    private static String resolve(ConfigurableEnvironment environment, String property) {
        try {
            return environment.getProperty(property);
        } catch (IllegalArgumentException unresolvablePlaceholder) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
