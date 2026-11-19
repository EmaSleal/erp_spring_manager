package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-045 (ADR-049 §3): contador de consultas automáticas sin resolver de un ENVIO_INCIERTO (consultas_error_hacienda). */
@DisplayName("T-045 — contador del ENVIO_INCIERTO: incremento, tope y reinicio")
class ContadorEnvioInciertoTest {

    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final HaciendaApiService hacienda = mock(HaciendaApiService.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(ComprobanteElectronicoServiceImpl.class);
    private ComprobanteElectronicoServiceImpl servicio;

    @BeforeEach
    void preparar() {
        ConfiguracionHaciendaService configuracion = mock(ConfiguracionHaciendaService.class);
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder().idConfiguracion(1).build();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configuracion.obtenerConfiguracionActiva())
                .thenReturn(ConfiguracionHacienda.builder().id(9L).empresa(empresa).build());
        servicio = new ComprobanteElectronicoServiceImpl(repository, null, null, configuracion, null, null, null, hacienda,
                null, mock(ComprobanteElectronicoMapper.class), null, null, Clock.systemDefaultZone(), null, null, null,
                (c, t) -> c.siguienteConsecutivo(t), CatalogoUbicacionDePrueba.validador());
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(logs);
    }

    private ComprobanteElectronico comprobante(EstadoComprobante estado, int contador, LocalDateTime fechaEnvio) {
        ComprobanteElectronico c = ComprobanteElectronico.builder().id(3L)
                .empresa(ConfiguracionEmpresa.builder().idConfiguracion(1).build())
                .claveNumerica("50613112600011820087800100001010000000028172833386").estado(estado)
                .consultasErrorHacienda(contador).fechaEnvio(fechaEnvio).build();
        when(repository.findById(3L)).thenReturn(Optional.of(c));
        return c;
    }

    private java.util.List<String> mensajes() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("registrarConsultaIncierta suma 1 al contador de un ENVIO_INCIERTO y lo guarda")
    void incrementa() {
        ComprobanteElectronico c = comprobante(EstadoComprobante.ENVIO_INCIERTO, 1, null);

        servicio.registrarConsultaIncierta(3L);

        assertThat(c.getConsultasErrorHacienda()).isEqualTo(2);
        verify(repository).save(c);
        assertThat(mensajes()).noneMatch(m -> m.contains("se deja de consultar"));
    }

    @Test
    @DisplayName("al llegar al máximo (5) lo dice el log: se deja de consultar")
    void alMaximoLoRegistra() {
        ComprobanteElectronico c = comprobante(EstadoComprobante.ENVIO_INCIERTO, 4, null);

        servicio.registrarConsultaIncierta(3L);

        assertThat(c.getConsultasErrorHacienda()).isEqualTo(5);
        assertThat(mensajes()).anyMatch(m -> m.contains("5 consultas automáticas sin resolver el ENVIO_INCIERTO")
                && m.contains("se deja de consultar"));
    }

    @Test
    @DisplayName("si el comprobante ya salió de ENVIO_INCIERTO, no se cuenta nada")
    void otroEstadoNoCuenta() {
        ComprobanteElectronico c = comprobante(EstadoComprobante.NO_TRANSMITIDO, 0, null);

        servicio.registrarConsultaIncierta(3L);

        assertThat(c.getConsultasErrorHacienda()).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("la consulta que deja el ENVIO_INCIERTO (ind-estado desconocido) conserva el contador; la que lo saca lo reinicia")
    void respuestaSinEstadoConservaYSalirReinicia() {
        ComprobanteElectronico c = comprobante(EstadoComprobante.ENVIO_INCIERTO, 3, null);
        when(hacienda.consultarComprobante(anyString(), anyLong()))
                .thenReturn(RespuestaHaciendaDTO.builder().indicadorEstado("desconocido").build());

        servicio.consultarYActualizarEstado(3L);
        assertThat(c.getEstado()).isEqualTo(EstadoComprobante.ENVIO_INCIERTO);
        assertThat(c.getConsultasErrorHacienda()).isEqualTo(3);

        when(hacienda.consultarComprobante(anyString(), anyLong()))
                .thenReturn(RespuestaHaciendaDTO.builder().indicadorEstado("recibido").build());
        servicio.consultarYActualizarEstado(3L);
        assertThat(c.getEstado()).isEqualTo(EstadoComprobante.PROCESANDO);
        assertThat(c.getConsultasErrorHacienda()).isZero();
    }

    @Test
    @DisplayName("ENVIO_INCIERTO → ERROR_HACIENDA: el contador de 'error' empieza en 1, no arrastra el de consultas sin resolver")
    void incertoAErrorHacienda_empiezaEnUno() {
        ComprobanteElectronico c = comprobante(EstadoComprobante.ENVIO_INCIERTO, 3, null);
        when(hacienda.consultarComprobante(anyString(), anyLong()))
                .thenReturn(RespuestaHaciendaDTO.builder().indicadorEstado("error").build());

        servicio.consultarYActualizarEstado(3L);

        assertThat(c.getEstado()).isEqualTo(EstadoComprobante.ERROR_HACIENDA);
        assertThat(c.getConsultasErrorHacienda()).isEqualTo(1);
    }

    @Test
    @DisplayName("ENVIO_INCIERTO con 404 pasados 15 min → NO_TRANSMITIDO y el contador vuelve a 0")
    void notFound_noTransmitido_reiniciaContador() {
        ComprobanteElectronico c = comprobante(EstadoComprobante.ENVIO_INCIERTO, 4, LocalDateTime.now().minusMinutes(20));
        when(hacienda.consultarComprobante(anyString(), anyLong()))
                .thenReturn(RespuestaHaciendaDTO.builder().documentoNoExiste(true).build());

        servicio.consultarYActualizarEstado(3L);

        assertThat(c.getEstado()).isEqualTo(EstadoComprobante.NO_TRANSMITIDO);
        assertThat(c.getConsultasErrorHacienda()).isZero();
    }
}
