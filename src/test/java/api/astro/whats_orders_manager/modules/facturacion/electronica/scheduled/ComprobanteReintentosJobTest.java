package api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-040 (criterio 4): el job registra el estado en que quedó cada reintento y solo cuenta como exitoso ENVIADO o posterior. */
@DisplayName("T-040 — ComprobanteReintentosJob: resultado real de cada reintento")
class ComprobanteReintentosJobTest {

    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final EnvioHaciendaService envio = mock(EnvioHaciendaService.class);
    private final ComprobanteReintentosJob job = new ComprobanteReintentosJob(repository,
        mock(ComprobanteElectronicoService.class), envio, Clock.systemUTC());
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(ComprobanteReintentosJob.class);

    @BeforeEach
    void capturarLog() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(logs);
    }

    private static ComprobanteElectronico pendiente(long id, EstadoComprobante estado, int intentos) {
        ComprobanteElectronico c = new ComprobanteElectronico();
        c.setId(id);
        c.setEstado(estado);
        c.setIntentosEnvio(intentos);
        return c;
    }

    private static ComprobanteElectronicoDTO resultado(EstadoComprobante estado, int intentos, String ultimoError) {
        return ComprobanteElectronicoDTO.builder().estado(estado).intentosEnvio(intentos).ultimoError(ultimoError).build();
    }

    private List<String> mensajes() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("ENVIADO cuenta como exitoso; ERROR y NO_TRANSMITIDO como fallidos, con su estado y ultimoError en el log")
    void cuentaSegunElEstadoEnQueQuedo() {
        when(repository.findPendientesReintento()).thenReturn(List.of(
            pendiente(1, EstadoComprobante.NO_TRANSMITIDO, 1),
            pendiente(2, EstadoComprobante.NO_TRANSMITIDO, 1),
            pendiente(3, EstadoComprobante.ERROR, 1)));
        when(envio.reenviar(1L)).thenReturn(resultado(EstadoComprobante.ENVIADO, 2, null));
        when(envio.reenviar(2L)).thenReturn(resultado(EstadoComprobante.NO_TRANSMITIDO, 2, "sin conexión"));
        when(envio.reenviar(3L)).thenReturn(resultado(EstadoComprobante.ERROR, 2, "Could not initialize proxy"));

        job.reintentarEnviosPendientes();

        assertThat(mensajes())
            .anyMatch(m -> m.contains("Comprobante 1 reintentado") && m.contains("ENVIADO"))
            .anyMatch(m -> m.contains("Comprobante 2 reintentado") && m.contains("NO_TRANSMITIDO") && m.contains("sin conexión"))
            .anyMatch(m -> m.contains("Comprobante 3 reintentado") && m.contains("ERROR") && m.contains("Could not initialize proxy"))
            .noneMatch(m -> m.contains("reenviado exitosamente"))
            .anyMatch(m -> m.contains("1 exitosos, 2 fallidos, 0 inciertos, 0 alcanzaron límite"));
        // El intento ya lo contó la marca de envío (intentos 1 → 2): no se cuenta dos veces
        verify(envio, never()).registrarFalloDeReintento(anyLong(), anyString());
    }

    @Test
    @DisplayName("ENVIO_INCIERTO: ni exitoso ni fallido, se cuenta aparte y no registra fallo; ACEPTADO es exitoso")
    void envioIncierto_seCuentaAparte() {
        when(repository.findPendientesReintento()).thenReturn(List.of(
            pendiente(1, EstadoComprobante.NO_TRANSMITIDO, 0), pendiente(2, EstadoComprobante.NO_TRANSMITIDO, 0)));
        when(envio.reenviar(1L)).thenReturn(resultado(EstadoComprobante.ENVIO_INCIERTO, 1, null));
        when(envio.reenviar(2L)).thenReturn(resultado(EstadoComprobante.ACEPTADO, 1, null));

        job.reintentarEnviosPendientes();

        assertThat(mensajes())
            .anyMatch(m -> m.contains("Comprobante 1 reintentado → ENVIO_INCIERTO (se resolverá por consulta)"))
            .anyMatch(m -> m.contains("1 exitosos, 0 fallidos, 1 inciertos, 0 alcanzaron límite"));
        verify(envio, never()).registrarFalloDeReintento(anyLong(), anyString());
    }

    @Test
    @DisplayName("falla local que no contó el intento (ERROR sin marca de envío): el job lo cuenta, para respetar el límite")
    void falloLocalSinMarcaCuentaElIntento() {
        when(repository.findPendientesReintento()).thenReturn(List.of(pendiente(7, EstadoComprobante.ERROR, 2)));
        when(envio.reenviar(7L)).thenReturn(resultado(EstadoComprobante.ERROR, 2, "Error generando el XML: x"));

        job.reintentarEnviosPendientes();

        verify(envio).registrarFalloDeReintento(7L, "Reintento 3: Error generando el XML: x");
        assertThat(mensajes()).anyMatch(m -> m.contains("0 exitosos, 1 fallidos, 0 inciertos"));
    }

    @Test
    @DisplayName("al límite de intentos no se reintenta y no cuenta como exitoso ni fallido")
    void alLimiteNoSeReintenta() {
        when(repository.findPendientesReintento()).thenReturn(List.of(pendiente(9, EstadoComprobante.NO_TRANSMITIDO, 5)));

        job.reintentarEnviosPendientes();

        verify(envio, never()).reenviar(anyLong());
        assertThat(mensajes()).anyMatch(m -> m.contains("0 exitosos, 0 fallidos, 0 inciertos, 1 alcanzaron límite"));
    }
}
