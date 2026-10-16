package api.astro.whats_orders_manager.config.crypto;

import api.astro.whats_orders_manager.config.startup.StartupConfigurationException;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ConfiguracionHaciendaServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-021 — Credenciales de Hacienda cifradas en reposo (H2). La clave es la de prueba de
 * src/test/resources/application.yml. Las columnas se leen con SQL nativo para ver lo que realmente se guarda.
 * Sin transacción de prueba: cada operación confirma como en producción.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({HaciendaCredencialesCifradoRunner.class, CredencialCipherConfig.class,
        ConfiguracionHaciendaServiceImpl.class, ConfiguracionHaciendaMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-021 — credenciales de Hacienda cifradas en reposo (H2)")
class HaciendaCredencialesCifradasJpaTest {

    private static final String SELECT_CREDENCIALES =
            "SELECT password_hacienda, pin_certificado, access_token, refresh_token FROM configuracion_hacienda WHERE id = ?";

    @MockitoBean private HaciendaApiService haciendaApiService;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private HaciendaCredencialesCifradoRunner runner;
    @Autowired private ConfiguracionHaciendaRepository haciendaRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ConfiguracionHaciendaService haciendaService;

    private ConfiguracionEmpresa empresa;

    @BeforeEach
    void preparar() {
        empresa = empresaRepository.save(ConfiguracionEmpresa.builder()
                .razonSocial("EMPRESA DE PRUEBA").nombreComercial("PRUEBA").build());
    }

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM configuracion_hacienda");
        empresaRepository.deleteAll();
    }

    private long insertarEnTextoPlano(String password, String pin, String access, String refresh) {
        jdbcTemplate.update("INSERT INTO configuracion_hacienda (empresa_id, ambiente, usuario_hacienda, "
                        + "password_hacienda, pin_certificado, access_token, refresh_token, activa, created_at) "
                        + "VALUES (?, 'SANDBOX', 'usuario-prueba', ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)",
                empresa.getIdConfiguracion(), password, pin, access, refresh);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM configuracion_hacienda", Long.class);
    }

    private Map<String, Object> columnas(long id) {
        return jdbcTemplate.queryForMap(SELECT_CREDENCIALES, id);
    }

    @Test
    @DisplayName("Arranque: filas en texto plano quedan con enc:v1: y JPA lee los valores originales")
    void migracion_cifraTextoPlano() {
        long completa = insertarEnTextoPlano("clave-plana", "pin-plano", "access-plano", "refresh-plano");
        long sinPin = insertarEnTextoPlano("otra-clave", null, null, null);

        int campos = runner.cifrarCredencialesEnTextoPlano();

        assertThat(campos).isEqualTo(5);
        assertThat(columnas(completa).values()).allSatisfy(v -> assertThat((String) v).startsWith("enc:v1:"));
        Map<String, Object> parcial = columnas(sinPin);
        assertThat((String) parcial.get("password_hacienda")).startsWith("enc:v1:");
        assertThat(parcial.get("pin_certificado")).isNull();
        assertThat(parcial.get("access_token")).isNull();

        ConfiguracionHacienda leida = haciendaRepository.findById(completa).orElseThrow();
        assertThat(leida.getPasswordHacienda()).isEqualTo("clave-plana");
        assertThat(leida.getPinCertificado()).isEqualTo("pin-plano");
        assertThat(leida.getAccessToken()).isEqualTo("access-plano");
        assertThat(leida.getRefreshToken()).isEqualTo("refresh-plano");
        assertThat(leida.toString()).doesNotContain("clave-plana", "pin-plano", "access-plano", "refresh-plano");
    }

    @Test
    @DisplayName("Arranque idempotente: valores ya cifrados no se tocan")
    void migracion_idempotente() {
        long id = insertarEnTextoPlano("clave-plana", "pin-plano", null, null);
        runner.cifrarCredencialesEnTextoPlano();
        Map<String, Object> primera = columnas(id);

        assertThat(runner.cifrarCredencialesEnTextoPlano()).isZero();
        assertThat(columnas(id)).isEqualTo(primera);
    }

    @Test
    @DisplayName("Guardar por JPA escribe cifrado")
    void jpa_escribeCifrado() {
        ConfiguracionHacienda guardada = haciendaRepository.save(ConfiguracionHacienda.builder()
                .empresa(empresa).ambiente(AmbienteHacienda.SANDBOX).usuarioHacienda("u")
                .passwordHacienda("clave-jpa").pinCertificado("9999").accessToken("tok").build());

        assertThat(columnas(guardada.getId()).values())
                .allSatisfy(v -> assertThat(v == null || ((String) v).startsWith("enc:v1:")).isTrue());
        assertThat(haciendaRepository.findById(guardada.getId()).orElseThrow().getPasswordHacienda())
                .isEqualTo("clave-jpa");
    }

    @Test
    @DisplayName("Editar con '********' conserva contraseña y PIN; un valor nuevo los reemplaza")
    void editarConMascara_conserva() {
        ConfiguracionHacienda guardada = haciendaRepository.save(ConfiguracionHacienda.builder()
                .empresa(empresa).ambiente(AmbienteHacienda.SANDBOX).usuarioHacienda("u")
                .passwordHacienda("clave-original").pinCertificado("4321").build());

        ConfiguracionHaciendaDTO dto = haciendaService.obtenerPorId(guardada.getId()).orElseThrow();
        assertThat(dto.getPasswordHacienda()).isEqualTo("********");
        assertThat(dto.getPinCertificado()).isEqualTo("********");
        dto.setUsuarioHacienda("u-editado");
        haciendaService.actualizar(guardada.getId(), dto);

        ConfiguracionHacienda tras = haciendaRepository.findById(guardada.getId()).orElseThrow();
        assertThat(tras.getUsuarioHacienda()).isEqualTo("u-editado");
        assertThat(tras.getPasswordHacienda()).isEqualTo("clave-original");
        assertThat(tras.getPinCertificado()).isEqualTo("4321");
        assertThat((String) columnas(guardada.getId()).get("password_hacienda")).startsWith("enc:v1:");

        dto.setPasswordHacienda("clave-nueva");
        dto.setPinCertificado(null);
        haciendaService.actualizar(guardada.getId(), dto);

        tras = haciendaRepository.findById(guardada.getId()).orElseThrow();
        assertThat(tras.getPasswordHacienda()).isEqualTo("clave-nueva");
        assertThat(tras.getPinCertificado()).as("PIN null en el PUT conserva el guardado").isEqualTo("4321");
    }

    @Test
    @DisplayName("Sin clave y con filas → no arranca, nombrando HACIENDA_CRYPTO_KEY; tabla vacía → arranca")
    void sinClave() {
        HaciendaCredencialesCifradoRunner sinClave =
                new HaciendaCredencialesCifradoRunner(jdbcTemplate, new CredencialCipher(""));

        assertThatCode(sinClave::cifrarCredencialesEnTextoPlano).doesNotThrowAnyException();

        insertarEnTextoPlano("clave-plana", null, null, null);
        assertThatThrownBy(sinClave::cifrarCredencialesEnTextoPlano)
                .isInstanceOf(StartupConfigurationException.class)
                .hasMessageContaining("HACIENDA_CRYPTO_KEY")
                .hasMessageContaining("1 fila");

        runner.cifrarCredencialesEnTextoPlano(); // con filas ya cifradas tampoco arranca sin clave
        assertThatThrownBy(sinClave::cifrarCredencialesEnTextoPlano)
                .isInstanceOf(StartupConfigurationException.class);
    }
}
