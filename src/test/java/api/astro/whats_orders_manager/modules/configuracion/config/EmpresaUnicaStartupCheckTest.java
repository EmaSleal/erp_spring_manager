package api.astro.whats_orders_manager.modules.configuracion.config;

import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-016 (ADR-019) — al arrancar se registra un ERROR si no hay exactamente una empresa activa.
 */
@DisplayName("T-016 — chequeo de empresa única al arrancar")
class EmpresaUnicaStartupCheckTest {

    private final EmpresaRepository repository = mock(EmpresaRepository.class);
    private final EmpresaUnicaStartupCheck check = new EmpresaUnicaStartupCheck(repository);

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void capturarLog() {
        logger = (Logger) LoggerFactory.getLogger(EmpresaUnicaStartupCheck.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(appender);
    }

    @ParameterizedTest(name = "{0} activas → ERROR")
    @ValueSource(longs = {0, 2})
    void distintoDeUna_registraError(long activas) {
        when(repository.contarEmpresasActivas()).thenReturn(activas);

        check.verificarEmpresaUnica();

        assertThat(appender.list)
                .filteredOn(e -> e.getLevel() == Level.ERROR)
                .singleElement()
                .satisfies(e -> assertThat(e.getFormattedMessage())
                        .contains("hay " + activas + " empresas activas"));
    }

    @Test
    @DisplayName("1 activa → no registra nada")
    void unaActiva_noRegistraNada() {
        when(repository.contarEmpresasActivas()).thenReturn(1L);

        check.verificarEmpresaUnica();

        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("un error de BD se registra como ERROR y no impide el arranque")
    void errorDeBd_noPropaga() {
        when(repository.contarEmpresasActivas()).thenThrow(new IllegalStateException("BD caída"));

        check.verificarEmpresaUnica();

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.ERROR);
    }
}
