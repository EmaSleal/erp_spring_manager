package api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled;

import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
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
import static org.mockito.ArgumentMatchers.any;
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
    private final AmbienteComprobanteGuard guard = mock(AmbienteComprobanteGuard.class);
    private final EnvioHaciendaService envio = mock(EnvioHaciendaService.class);
    private final ComprobanteReintentosJob job = new ComprobanteReintentosJob(repository,
        mock(ComprobanteElectronicoService.class), envio, Clock.systemUTC(), guard);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(ComprobanteReintentosJob.class);

    @BeforeEach
    void capturarLog() {
        when(guard.ambienteActivo()).thenReturn(java.util.Optional.of(AmbienteHacienda.SANDBOX));
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
        when(repository.findPendientesReintento(AmbienteHacienda.SANDBOX)).thenReturn(List.of(
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
        when(repository.findPendientesReintento(AmbienteHacienda.SANDBOX)).thenReturn(List.of(
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
        when(repository.findPendientesReintento(AmbienteHacienda.SANDBOX)).thenReturn(List.of(pendiente(7, EstadoComprobante.ERROR, 2)));
        when(envio.reenviar(7L)).thenReturn(resultado(EstadoComprobante.ERROR, 2, "Error generando el XML: x"));

        job.reintentarEnviosPendientes();

        verify(envio).registrarFalloDeReintento(7L, "Reintento 3: Error generando el XML: x");
        assertThat(mensajes()).anyMatch(m -> m.contains("0 exitosos, 1 fallidos, 0 inciertos"));
    }

    @Test
    @DisplayName("al límite de intentos no se reintenta y no cuenta como exitoso ni fallido")
    void alLimiteNoSeReintenta() {
        when(repository.findPendientesReintento(AmbienteHacienda.SANDBOX)).thenReturn(List.of(pendiente(9, EstadoComprobante.NO_TRANSMITIDO, 5)));

        job.reintentarEnviosPendientes();

        verify(envio, never()).reenviar(anyLong());
        assertThat(mensajes()).anyMatch(m -> m.contains("0 exitosos, 0 fallidos, 0 inciertos, 1 alcanzaron límite"));
    }

    @Test
    @DisplayName("T-044 criterio 4: el job filtra por el ambiente activo e informa cuántos excluyó por ambiente")
    void reintentos_excluidosPorAmbiente_enElLog() {
        when(guard.ambienteActivo()).thenReturn(java.util.Optional.of(AmbienteHacienda.PRODUCCION));
        when(repository.findPendientesReintento(AmbienteHacienda.PRODUCCION)).thenReturn(List.of());
        when(repository.countPendientesReintentoDeOtroAmbiente(AmbienteHacienda.PRODUCCION)).thenReturn(3L);

        job.reintentarEnviosPendientes();

        assertThat(mensajes()).anyMatch(m -> m.contains("3 excluidos por ambiente") && m.contains("PRODUCCION"));
        verify(repository, never()).findPendientesReintento(AmbienteHacienda.SANDBOX);
        verify(repository, never()).findPendientesReintentoSinFiltroDeAmbiente();
        verify(envio, never()).reenviar(anyLong());
    }

    @Test
    @DisplayName("T-044: ambiente distinto detectado al reenviar (carrera) → se omite, sin registrarFalloDeReintento")
    void ambienteDistinto_noRegistraFallo() {
        when(repository.findPendientesReintento(AmbienteHacienda.SANDBOX))
            .thenReturn(List.of(pendiente(4, EstadoComprobante.NO_TRANSMITIDO, 1)));
        when(envio.reenviar(4L)).thenThrow(new api.astro.whats_orders_manager.modules.facturacion.electronica.exception
            .AmbienteDistintoException(AmbienteHacienda.PRODUCCION, AmbienteHacienda.SANDBOX));

        job.reintentarEnviosPendientes();

        verify(envio, never()).registrarFalloDeReintento(anyLong(), anyString());
        assertThat(mensajes()).anyMatch(m -> m.contains("Comprobante 4 es de otro ambiente"))
            .anyMatch(m -> m.contains("0 exitosos, 0 fallidos"));
    }

    @Test
    @DisplayName("T-044: sin configuración activa legible, el job conserva su comportamiento (sin filtro) y lo registra")
    void ambienteIlegible_conservaElComportamiento() {
        when(guard.ambienteActivo()).thenReturn(java.util.Optional.empty());
        when(repository.findPendientesReintentoSinFiltroDeAmbiente()).thenReturn(List.of());

        job.reintentarEnviosPendientes();

        verify(repository).findPendientesReintentoSinFiltroDeAmbiente();
        assertThat(mensajes()).anyMatch(m -> m.contains("ambiente activo no disponible"));
    }

    @Test
    @DisplayName("T-044 criterio 4: la consulta horaria filtra por el ambiente activo, informa los excluidos y no cuenta el ambiente distinto como error")
    void consultaHoraria_filtraPorAmbiente() {
        var servicio = mock(ComprobanteElectronicoService.class);
        ComprobanteReintentosJob conServicio = new ComprobanteReintentosJob(repository, servicio, envio,
            Clock.systemUTC(), guard);
        when(repository.findParaSondeo(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(AmbienteHacienda.SANDBOX)))
            .thenReturn(List.of(pendiente(5, EstadoComprobante.ENVIADO, 1)));
        when(repository.countParaSondeoDeOtroAmbiente(any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.eq(AmbienteHacienda.SANDBOX))).thenReturn(2L);
        when(servicio.consultarYActualizarEstado(5L)).thenThrow(new api.astro.whats_orders_manager.modules.facturacion
            .electronica.exception.AmbienteDistintoException(AmbienteHacienda.PRODUCCION, AmbienteHacienda.SANDBOX));

        conServicio.consultarEstadosPendientes();

        assertThat(mensajes()).anyMatch(m -> m.contains("2 excluidos por ambiente"))
            .anyMatch(m -> m.contains("Comprobante 5 es de otro ambiente"))
            .noneMatch(m -> m.contains("Error consultando estado"));
    }

    private ComprobanteReintentosJob jobConServicio(ComprobanteElectronicoService servicio) {
        return new ComprobanteReintentosJob(repository, servicio, envio, Clock.systemUTC(), guard);
    }

    @Test
    @DisplayName("T-045: el job suma los ENVIO_INCIERTO elegibles a los de siempre y los consulta con consultarYActualizarEstado")
    void consultaHoraria_incluyeLosInciertos() {
        var servicio = mock(ComprobanteElectronicoService.class);
        when(repository.findParaSondeo(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(AmbienteHacienda.SANDBOX)))
            .thenReturn(List.of(pendiente(5, EstadoComprobante.ENVIADO, 1)));
        when(repository.findInciertosParaSondeo(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(AmbienteHacienda.SANDBOX)))
            .thenReturn(List.of(pendiente(6, EstadoComprobante.ENVIO_INCIERTO, 1)));
        when(servicio.consultarYActualizarEstado(5L)).thenReturn(resultado(EstadoComprobante.PROCESANDO, 1, null));
        when(servicio.consultarYActualizarEstado(6L)).thenReturn(resultado(EstadoComprobante.ACEPTADO, 1, null));

        jobConServicio(servicio).consultarEstadosPendientes();

        verify(servicio).consultarYActualizarEstado(5L);
        verify(servicio).consultarYActualizarEstado(6L);
        verify(servicio, never()).registrarConsultaIncierta(anyLong());
        assertThat(mensajes()).anyMatch(m -> m.contains("Estados consultados: 2/2"));
    }

    @Test
    @DisplayName("T-045: la ventana de los ENVIO_INCIERTO es de 15 min (MINUTOS_ANTES_DE_VERIFICAR) y la de los demás, de 5")
    void consultaHoraria_ventanas() {
        var servicio = mock(ComprobanteElectronicoService.class);
        var limiteGeneral = org.mockito.ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        var limiteIncierto = org.mockito.ArgumentCaptor.forClass(java.time.LocalDateTime.class);

        jobConServicio(servicio).consultarEstadosPendientes();

        verify(repository).findParaSondeo(limiteGeneral.capture(), org.mockito.ArgumentMatchers.anyInt(), any());
        verify(repository).findInciertosParaSondeo(limiteIncierto.capture(), org.mockito.ArgumentMatchers.anyInt(), any());
        assertThat(java.time.Duration.between(limiteIncierto.getValue(), limiteGeneral.getValue()).toMinutes()).isEqualTo(
            api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl
                .MINUTOS_ANTES_DE_VERIFICAR - 5);
    }

    @Test
    @DisplayName("T-045: un ENVIO_INCIERTO que la consulta deja igual, o que falla con excepción, cuenta (registrarConsultaIncierta); uno que sale del estado, no")
    void consultaHoraria_cuentaLasConsultasSinResolver() {
        var servicio = mock(ComprobanteElectronicoService.class);
        when(repository.findInciertosParaSondeo(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(AmbienteHacienda.SANDBOX)))
            .thenReturn(List.of(pendiente(1, EstadoComprobante.ENVIO_INCIERTO, 0), pendiente(2, EstadoComprobante.ENVIO_INCIERTO, 0),
                pendiente(3, EstadoComprobante.ENVIO_INCIERTO, 0)));
        when(repository.findParaSondeo(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(AmbienteHacienda.SANDBOX)))
            .thenReturn(List.of(pendiente(4, EstadoComprobante.ENVIADO, 0)));
        when(servicio.consultarYActualizarEstado(1L)).thenReturn(resultado(EstadoComprobante.ENVIO_INCIERTO, 1, null));
        when(servicio.consultarYActualizarEstado(2L)).thenThrow(new RuntimeException("sin red"));
        when(servicio.consultarYActualizarEstado(3L)).thenReturn(resultado(EstadoComprobante.NO_TRANSMITIDO, 1, null));
        // un ENVIADO cuya consulta lo deja en ENVIO_INCIERTO (404) no cuenta: venía de otro estado
        when(servicio.consultarYActualizarEstado(4L)).thenReturn(resultado(EstadoComprobante.ENVIO_INCIERTO, 1, null));

        jobConServicio(servicio).consultarEstadosPendientes();

        verify(servicio).registrarConsultaIncierta(1L);
        verify(servicio).registrarConsultaIncierta(2L);
        verify(servicio, never()).registrarConsultaIncierta(3L);
        verify(servicio, never()).registrarConsultaIncierta(4L);
    }

    @Test
    @DisplayName("T-045: ambiente distinto en la consulta de un ENVIO_INCIERTO → no se cuenta; el excluido por ambiente suma los dos conteos")
    void consultaHoraria_ambienteDistintoNoCuenta() {
        var servicio = mock(ComprobanteElectronicoService.class);
        when(repository.findInciertosParaSondeo(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(AmbienteHacienda.SANDBOX)))
            .thenReturn(List.of(pendiente(1, EstadoComprobante.ENVIO_INCIERTO, 0)));
        when(repository.countParaSondeoDeOtroAmbiente(any(), org.mockito.ArgumentMatchers.anyInt(), any())).thenReturn(1L);
        when(repository.countInciertosParaSondeoDeOtroAmbiente(any(), org.mockito.ArgumentMatchers.anyInt(), any())).thenReturn(2L);
        when(servicio.consultarYActualizarEstado(1L)).thenThrow(new api.astro.whats_orders_manager.modules.facturacion
            .electronica.exception.AmbienteDistintoException(AmbienteHacienda.PRODUCCION, AmbienteHacienda.SANDBOX));

        jobConServicio(servicio).consultarEstadosPendientes();

        verify(servicio, never()).registrarConsultaIncierta(anyLong());
        assertThat(mensajes()).anyMatch(m -> m.contains("3 excluidos por ambiente"));
    }

    @Test
    @DisplayName("T-045: con la configuración ilegible, los ENVIO_INCIERTO también se consultan sin filtro de ambiente (T-041)")
    void consultaHoraria_configuracionIlegible_incluyeInciertos() {
        var servicio = mock(ComprobanteElectronicoService.class);
        when(guard.ambienteActivo()).thenReturn(java.util.Optional.empty());
        when(repository.findInciertosParaSondeoSinFiltroDeAmbiente(any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(List.of(pendiente(8, EstadoComprobante.ENVIO_INCIERTO, 0)));
        when(servicio.consultarYActualizarEstado(8L)).thenReturn(resultado(EstadoComprobante.ACEPTADO, 1, null));

        jobConServicio(servicio).consultarEstadosPendientes();

        verify(servicio).consultarYActualizarEstado(8L);
    }
}
