package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.config.crypto.CredencialCipherConfig;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmailRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.ConfiguracionEmailServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-022 (ADR-027) — configuracion_email de fila única: un solo método de lectura, ninguna ruta de guardado
 * persiste la máscara "********" (ni el vacío en edición) y no se crea una segunda fila. H2; la contraseña se
 * compara descifrada (JPA) y por SQL nativo solo para ver que sigue cifrada.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({ConfiguracionEmailServiceImpl.class, CredencialCipherConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-022 — configuracion_email de fila única")
class ConfiguracionEmailFilaUnicaJpaTest {

    private static final String PASSWORD = "clave-smtp-original";
    private static final String MASCARA = "********";

    @Autowired private ConfiguracionEmailService service;
    @Autowired private ConfiguracionEmailRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private ConfiguracionEmail existente;

    @BeforeEach
    void preparar() {
        existente = repository.save(ConfiguracionEmail.builder()
                .smtpHost("smtp.example.invalid").smtpPort(587).smtpUsuario("cuenta@example.invalid")
                .smtpPassword(PASSWORD).emailRemitente("cuenta@example.invalid").activo(false).build());
    }

    @AfterEach
    void limpiar() {
        repository.deleteAll();
    }

    private String passwordGuardada() {
        return repository.findById(existente.getIdConfiguracion()).orElseThrow().getSmtpPassword();
    }

    private ConfiguracionEmail cambios(String password) {
        return ConfiguracionEmail.builder().smtpHost("smtp2.example.invalid").smtpPort(465)
                .smtpUsuario("cuenta@example.invalid").smtpPassword(password)
                .emailRemitente("cuenta@example.invalid").activo(null).smtpSsl(null).smtpTls(null).smtpAuth(null)
                .smtpTimeout(null).charset(null).build();
    }

    @Test
    @DisplayName("actualizarConfiguracion con '********' o vacío conserva la contraseña; un valor nuevo la reemplaza")
    void actualizar_conMascara_conserva() {
        service.actualizarConfiguracion(cambios(MASCARA));
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);

        service.actualizarConfiguracion(cambios(""));
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);

        service.actualizarConfiguracion(cambios(null));
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);

        service.actualizarConfiguracion(cambios("clave-nueva"));
        assertThat(passwordGuardada()).isEqualTo("clave-nueva");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("saveOrUpdate con '********' conserva la contraseña y aplica los demás campos")
    void saveOrUpdate_conMascara_conserva() {
        service.saveOrUpdate(cambios(MASCARA));

        ConfiguracionEmail tras = repository.findById(existente.getIdConfiguracion()).orElseThrow();
        assertThat(tras.getSmtpPassword()).isEqualTo(PASSWORD);
        assertThat(tras.getSmtpHost()).isEqualTo("smtp2.example.invalid");
        assertThat(jdbcTemplate.queryForObject("SELECT smtp_password FROM configuracion_email", String.class))
                .startsWith("enc:v1:");
    }

    @Test
    @DisplayName("guardarConfiguracion de la fila existente con '********' conserva la contraseña")
    void guardar_conMascara_conserva() {
        ConfiguracionEmail edicion = cambios(MASCARA);
        edicion.setIdConfiguracion(existente.getIdConfiguracion());
        edicion.setActivo(false);

        service.guardarConfiguracion(edicion);

        assertThat(passwordGuardada()).isEqualTo(PASSWORD);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("guardarConfiguracion de una fila nueva con una ya existente → IllegalStateException, sin segunda fila")
    void guardar_segundaFila_rechazada() {
        assertThatThrownBy(() -> service.guardarConfiguracion(cambios("otra-clave")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("una sola");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("obtenerOCrearConfiguracion devuelve la existente; solo crea con la tabla vacía (y una sola vez)")
    void obtenerOCrear_nuncaSegundaFila() {
        assertThat(service.obtenerOCrearConfiguracion().getIdConfiguracion()).isEqualTo(existente.getIdConfiguracion());
        assertThat(repository.count()).isEqualTo(1);

        repository.deleteAll();
        ConfiguracionEmail creada = service.obtenerOCrearConfiguracion();
        assertThat(creada.getActivo()).isFalse();
        assertThat(service.obtenerOCrearConfiguracion().getIdConfiguracion()).isEqualTo(creada.getIdConfiguracion());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Lectura única: la fila inactiva es la configuración (activo = envío habilitado, no cuál fila)")
    void lecturaUnica_noDependeDeActivo() {
        assertThat(service.obtenerConfiguracion()).get()
                .extracting(ConfiguracionEmail::getIdConfiguracion).isEqualTo(existente.getIdConfiguracion());
        assertThat(service.validarConfiguracion()).isTrue();

        service.cambiarEstado(true);
        assertThat(service.obtenerConfiguracion().orElseThrow().getActivo()).isTrue();
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);
    }
}
