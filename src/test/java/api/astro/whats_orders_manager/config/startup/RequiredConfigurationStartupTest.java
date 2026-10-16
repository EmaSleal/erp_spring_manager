package api.astro.whats_orders_manager.config.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-021 — Arranque real de SpringApplication: el EnvironmentPostProcessor y el FailureAnalyzer están registrados
 * en META-INF/spring.factories; sin variables, el arranque falla con el mensaje legible (no un stacktrace de
 * Hikari) y no se crea ningún DataSource (el contexto ni siquiera se refresca).
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("T-021 — arranque sin variables obligatorias")
class RequiredConfigurationStartupTest {

    @Configuration
    static class Empty {
    }

    @Test
    @DisplayName("Sin variables → StartupConfigurationException y análisis 'APPLICATION FAILED TO START'")
    void sinVariables_fallaConMensajeClaro(CapturedOutput output) {
        SpringApplication app = new SpringApplication(Empty.class);

        assertThatThrownBy(() -> app.run("--spring.config.location=classpath:t021/sin-variables.yml"))
                .isInstanceOf(StartupConfigurationException.class)
                .hasMessageContaining("DB_URL");

        assertThat(output).contains("APPLICATION FAILED TO START")
                .contains("Faltan variables de entorno obligatorias")
                .doesNotContain("HikariPool");
    }
}
