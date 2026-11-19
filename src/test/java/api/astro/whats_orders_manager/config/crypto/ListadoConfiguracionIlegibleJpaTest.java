package api.astro.whats_orders_manager.config.crypto;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-041 — el listado de configuraciones con una fila de credenciales ilegibles, dentro de una misma transacción y
 * EntityManager (como open-in-view): la fila legible sale completa, la ilegible con el aviso fijo, y la transacción
 * del llamador sigue sana. Los datos se confirman antes (sin transacción de prueba).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({CredencialCipherConfig.class, ConfiguracionHaciendaServiceImpl.class, ConfiguracionHaciendaMapper.class,
        CertificadoHaciendaValidator.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-041 — listado con una configuración de credenciales ilegibles (H2)")
class ListadoConfiguracionIlegibleJpaTest {

    @MockitoBean private HaciendaApiService haciendaApiService;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ConfiguracionHaciendaService servicio;
    @Autowired private PlatformTransactionManager transactionManager;

    private ConfiguracionEmpresa empresa;

    @BeforeEach
    void preparar() {
        empresa = empresaRepository.save(ConfiguracionEmpresa.builder().razonSocial("EMPRESA").nombreComercial("E").build());
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM configuracion_hacienda");
        empresaRepository.deleteAll();
    }

    private long fila(String usuario, String password) {
        jdbc.update("INSERT INTO configuracion_hacienda (empresa_id, ambiente, usuario_hacienda, password_hacienda, "
                + "activa, created_at) VALUES (?, 'SANDBOX', ?, ?, FALSE, CURRENT_TIMESTAMP)",
                empresa.getIdConfiguracion(), usuario, password);
        return jdbc.queryForObject("SELECT MAX(id) FROM configuracion_hacienda", Long.class);
    }

    @Test
    @DisplayName("dos filas, una legible y una alterada, listadas en una sola transacción: la legible completa, la otra marcada")
    void unaLegibleYUnaIlegible_enUnaSolaTransaccion() {
        long legible = fila("usuario-legible", "clave-plana-heredada"); // sin prefijo: texto plano legible
        long ilegible = fila("usuario-ilegible", "enc:v1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ConfiguracionHaciendaServiceImpl.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        boolean[] rollbackOnly = new boolean[1];
        List<ConfiguracionHaciendaDTO> lista = new TransactionTemplate(transactionManager).execute(st -> {
            List<ConfiguracionHaciendaDTO> resultado = servicio.listarPorEmpresa(empresa.getIdConfiguracion());
            rollbackOnly[0] = st.isRollbackOnly(); // la lectura fallida no debe contaminar la transacción del llamador
            return resultado;
        });
        logger.detachAppender(appender);
        assertThat(rollbackOnly[0]).isFalse();
        // La causa registrada es la del descifrado, no un efecto de una entidad a medias en el EntityManager compartido
        assertThat(appender.list).extracting(e -> e.getFormattedMessage())
                .noneMatch(m -> m.contains("NullPointerException"))
                .anyMatch(m -> m.contains("IllegalStateException"));

        assertThat(lista).hasSize(2);
        ConfiguracionHaciendaDTO ok = lista.stream().filter(d -> d.getId() == legible).findFirst().orElseThrow();
        ConfiguracionHaciendaDTO mala = lista.stream().filter(d -> d.getId() == ilegible).findFirst().orElseThrow();
        assertThat(ok.getCredencialesIlegibles()).isFalse();
        assertThat(ok.getAvisoCredenciales()).isNull();
        assertThat(ok.getUsuarioHacienda()).isEqualTo("usuario-legible");
        assertThat(mala.getCredencialesIlegibles()).isTrue();
        assertThat(mala.getAvisoCredenciales()).isEqualTo(ConfiguracionHaciendaMapper.AVISO_CREDENCIALES_ILEGIBLES);
        assertThat(mala.getUsuarioHacienda()).isEqualTo("usuario-ilegible");
    }
}
