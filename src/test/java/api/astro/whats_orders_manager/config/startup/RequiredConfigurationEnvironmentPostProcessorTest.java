package api.astro.whats_orders_manager.config.startup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * T-021 — Sin valores por defecto: si falta DB_URL, DB_USERNAME, DB_PASSWORD o META_WEBHOOK_VERIFY_TOKEN el
 * arranque falla con un mensaje que nombra las variables. MockEnvironment no incluye variables de entorno del
 * sistema, así que la prueba no depende de la máquina.
 */
@DisplayName("T-021 — configuración obligatoria al arrancar")
class RequiredConfigurationEnvironmentPostProcessorTest {

    private final RequiredConfigurationEnvironmentPostProcessor processor =
            new RequiredConfigurationEnvironmentPostProcessor();
    private MockEnvironment environment;

    /** Mismas expresiones que src/main/resources/application.yml. */
    @BeforeEach
    void comoApplicationYml() {
        environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "${DB_URL}")
                .withProperty("spring.datasource.username", "${DB_USERNAME}")
                .withProperty("spring.datasource.password", "${DB_PASSWORD}")
                .withProperty("meta.webhook.verify-token", "${META_WEBHOOK_VERIFY_TOKEN}")
                .withProperty("hacienda.crypto.key", "${HACIENDA_CRYPTO_KEY:}");
    }

    private StartupConfigurationException fallo() {
        return catchThrowableOfType(StartupConfigurationException.class,
                () -> processor.postProcessEnvironment(environment, new SpringApplication()));
    }

    private void conTodo() {
        environment.withProperty("DB_URL", "jdbc:mysql://127.0.0.1:3306/t021")
                .withProperty("DB_USERNAME", "app")
                .withProperty("DB_PASSWORD", "x")
                .withProperty("META_WEBHOOK_VERIFY_TOKEN", "token");
    }

    @Test
    @DisplayName("Nada definido → falla nombrando las cuatro variables")
    void nadaDefinido_nombraTodas() {
        StartupConfigurationException e = fallo();

        assertThat(e).isNotNull();
        assertThat(e.getDescription())
                .contains("DB_URL", "DB_USERNAME", "DB_PASSWORD", "META_WEBHOOK_VERIFY_TOKEN");
        assertThat(e.getAction()).contains("DB_URL").contains(".env");
    }

    @Test
    @DisplayName("Solo falta DB_URL → solo nombra DB_URL")
    void soloFaltaDbUrl() {
        conTodo();
        environment.setProperty("DB_URL", "   ");

        StartupConfigurationException e = fallo();

        assertThat(e).isNotNull();
        assertThat(e.getDescription()).contains("DB_URL")
                .doesNotContain("DB_USERNAME").doesNotContain("DB_PASSWORD").doesNotContain("META_WEBHOOK");
    }

    @Test
    @DisplayName("Token del webhook vacío → falla nombrando META_WEBHOOK_VERIFY_TOKEN")
    void tokenVacio_falla() {
        conTodo();
        environment.setProperty("META_WEBHOOK_VERIFY_TOKEN", "");

        assertThat(fallo().getDescription()).contains("META_WEBHOOK_VERIFY_TOKEN");
    }

    @Test
    @DisplayName("DB_PASSWORD vacía está permitida; todo definido → arranca")
    void passwordVacia_permitida() {
        conTodo();
        environment.setProperty("DB_PASSWORD", "");

        assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Propiedades dadas directamente (sin placeholder, p. ej. el yml de pruebas) → arranca")
    void propiedadesDirectas() {
        environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:x")
                .withProperty("spring.datasource.username", "sa")
                .withProperty("spring.datasource.password", "")
                .withProperty("meta.webhook.verify-token", "t");

        assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HACIENDA_CRYPTO_KEY inválida → falla sin mostrar la clave; ausente → permitido")
    void claveDeCifrado() {
        conTodo();
        assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .doesNotThrowAnyException();

        environment.setProperty("HACIENDA_CRYPTO_KEY", "c2hvcnQta2V5"); // 9 bytes
        StartupConfigurationException corta = fallo();
        assertThat(corta.getDescription()).contains("HACIENDA_CRYPTO_KEY").contains("32 bytes")
                .doesNotContain("c2hvcnQta2V5");

        environment.setProperty("HACIENDA_CRYPTO_KEY", "no es base64!!");
        assertThat(fallo().getDescription()).contains("Base64").doesNotContain("no es base64!!");
    }

    @Test
    @DisplayName("El FailureAnalyzer muestra descripción y acción")
    void failureAnalyzer() {
        StartupConfigurationException e = fallo();

        FailureAnalysis analysis = new StartupConfigurationFailureAnalyzer()
                .analyze(new IllegalStateException("envoltura", e));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("DB_URL");
        assertThat(analysis.getAction()).contains("Defina");
    }
}
