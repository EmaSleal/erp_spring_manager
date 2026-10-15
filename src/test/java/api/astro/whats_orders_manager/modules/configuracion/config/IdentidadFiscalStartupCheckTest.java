package api.astro.whats_orders_manager.modules.configuracion.config;

import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-018 (ADR-021) — al arrancar se registra un ERROR si la identidad fiscal no es única, si no hay
 * exactamente una configuración de Hacienda activa o si su certificado no existe; nunca credenciales.
 */
@DisplayName("T-018 — chequeo de identidad fiscal al arrancar")
class IdentidadFiscalStartupCheckTest {

    private static final String CLAVE_DE_PRUEBA = "clave-de-prueba-no-debe-aparecer";

    private final ConfiguracionEmpresaRepository empresaRepository = mock(ConfiguracionEmpresaRepository.class);
    private final ConfiguracionHaciendaRepository haciendaRepository = mock(ConfiguracionHaciendaRepository.class);
    private final IdentidadFiscalStartupCheck check = new IdentidadFiscalStartupCheck(empresaRepository, haciendaRepository);

    @TempDir Path dir;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void capturarLog() {
        logger = (Logger) LoggerFactory.getLogger(IdentidadFiscalStartupCheck.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(appender);
    }

    private ConfiguracionHacienda hacienda(long id, AmbienteHacienda ambiente, String ruta) {
        return ConfiguracionHacienda.builder()
                .id(id).ambiente(ambiente).activa(true).rutaCertificado(ruta)
                .usuarioHacienda("usuario-de-prueba").passwordHacienda(CLAVE_DE_PRUEBA).pinCertificado(CLAVE_DE_PRUEBA)
                .accessToken(CLAVE_DE_PRUEBA)
                .build();
    }

    private Path certificado() throws Exception {
        return Files.writeString(dir.resolve("certificado.p12"), "contenido de prueba");
    }

    private List<String> errores() {
        return appender.list.stream().filter(e -> e.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("estado válido (1 empresa, 1 activa, certificado legible) → no registra nada")
    void estadoValido_noRegistraNada() throws Exception {
        when(empresaRepository.count()).thenReturn(1L);
        when(haciendaRepository.findByActivaTrueOrderByIdAsc())
                .thenReturn(List.of(hacienda(3, AmbienteHacienda.SANDBOX, certificado().toString())));

        check.verificarIdentidadFiscal();

        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("2 configuraciones de Hacienda activas → ERROR con las ids")
    void dosActivas_registraError() throws Exception {
        when(empresaRepository.count()).thenReturn(1L);
        when(haciendaRepository.findByActivaTrueOrderByIdAsc()).thenReturn(List.of(
                hacienda(3, AmbienteHacienda.SANDBOX, certificado().toString()),
                hacienda(4, AmbienteHacienda.PRODUCCION, certificado().toString())));

        check.verificarIdentidadFiscal();

        assertThat(errores()).singleElement().asString()
                .contains("hay 2 configuraciones de Hacienda activas (ids [3, 4])");
    }

    @Test
    @DisplayName("2 filas en configuracion_empresa → ERROR")
    void dosEmpresas_registraError() throws Exception {
        when(empresaRepository.count()).thenReturn(2L);
        when(haciendaRepository.findByActivaTrueOrderByIdAsc())
                .thenReturn(List.of(hacienda(3, AmbienteHacienda.SANDBOX, certificado().toString())));

        check.verificarIdentidadFiscal();

        assertThat(errores()).singleElement().asString().contains("configuracion_empresa tiene 2 filas");
    }

    @Test
    @DisplayName("certificado inexistente (ruta de Windows en Linux) → ERROR sin credenciales")
    void certificadoInexistente_registraErrorSinCredenciales() {
        when(empresaRepository.count()).thenReturn(1L);
        when(haciendaRepository.findByActivaTrueOrderByIdAsc()).thenReturn(List.of(
                hacienda(3, AmbienteHacienda.SANDBOX, "D:\\programacion\\certificados\\certificado.p12")));

        check.verificarIdentidadFiscal();

        assertThat(errores()).singleElement().asString()
                .contains("no existe o no se puede leer")
                .contains("id 3, SANDBOX");
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(m -> m.contains(CLAVE_DE_PRUEBA));
    }

    @Test
    @DisplayName("ninguna configuración activa → ERROR")
    void ningunaActiva_registraError() {
        when(empresaRepository.count()).thenReturn(1L);
        when(haciendaRepository.findByActivaTrueOrderByIdAsc()).thenReturn(List.of());

        check.verificarIdentidadFiscal();

        assertThat(errores()).singleElement().asString().contains("hay 0 configuraciones de Hacienda activas");
    }
}
