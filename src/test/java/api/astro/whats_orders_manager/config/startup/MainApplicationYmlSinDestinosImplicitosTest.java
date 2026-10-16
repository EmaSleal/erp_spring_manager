package api.astro.whats_orders_manager.config.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-021 — El application.yml de producción (leído del disco: en el classpath de pruebas lo sombrea
 * src/test/resources/application.yml) no trae destinos ni secretos por defecto.
 */
@DisplayName("T-021 — application.yml sin destinos implícitos")
class MainApplicationYmlSinDestinosImplicitosTest {

    @Test
    void sinValoresPorDefectoDeBaseNiToken() throws Exception {
        String yml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertThat(yml)
                .doesNotContain("192.168.100.93")
                .doesNotContain(":root}")
                .doesNotContain(":password}")
                .doesNotContain("defaulttoken")
                .contains("url: ${DB_URL}")
                .contains("username: ${DB_USERNAME}")
                .contains("password: ${DB_PASSWORD}")
                .contains("verify-token: ${META_WEBHOOK_VERIFY_TOKEN}");
    }
}
