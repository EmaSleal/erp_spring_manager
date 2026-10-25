package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConsultaManualNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.LimiteConsultaManualHacienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-026 (ADR-031 §4) — "Consultar estado en Hacienda" con un reloj inyectado fijo: la espera de 15 minutos ya no
 * impide consultar; solo decide si un 404 de un ENVIO_INCIERTO significa NO_TRANSMITIDO. El cliente de Hacienda es
 * un mock: ninguna llamada real.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("T-026 — consulta manual de estado: regla del 404 de ENVIO_INCIERTO con reloj inyectado")
class ConsultaManualHaciendaRelojTest {

    private static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");
    private static final LocalDateTime ENVIO = LocalDateTime.of(2026, 10, 17, 10, 0);

    @Mock private ComprobanteElectronicoRepository repository;
    @Mock private ConfiguracionHaciendaService configuracionService;
    @Mock private HaciendaApiService haciendaApi;
    @Mock private ComprobanteElectronicoMapper mapper;
    @Mock private LimiteConsultaManualHacienda limiteConsultaManual;
    @Mock private Clock clock;

    @InjectMocks private ComprobanteElectronicoServiceImpl service;

    private ComprobanteElectronico comprobante;

    @BeforeEach
    void setUp() {
        ConfiguracionEmpresa empresa = new ConfiguracionEmpresa();
        empresa.setIdConfiguracion(1);
        comprobante = ComprobanteElectronico.builder()
                .id(5L).empresa(empresa).claveNumerica("506" + "0".repeat(47))
                .estado(EstadoComprobante.ENVIO_INCIERTO).fechaEnvio(ENVIO).build();
        when(repository.findById(5L)).thenReturn(Optional.of(comprobante));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configuracionService.obtenerConfiguracionActiva())
                .thenReturn(ConfiguracionHacienda.builder().id(1L).empresa(empresa).build());
        when(clock.getZone()).thenReturn(ZONA);
        when(haciendaApi.consultarComprobante(anyString(), anyLong()))
                .thenReturn(RespuestaHaciendaDTO.builder().documentoNoExiste(true).codigoHttp(404).build());
    }

    private void ahora(LocalDateTime momento) {
        when(clock.instant()).thenReturn(momento.atZone(ZONA).toInstant());
    }

    @Test
    @DisplayName("14 min 59 s después del envío: consulta; 404 → sigue ENVIO_INCIERTO con la hora (10:15) en el mensaje")
    void antesDe15Minutos_404_sinCambios() {
        ahora(ENVIO.plusMinutes(14).plusSeconds(59));

        var resultado = service.consultarEstadoManual(5L);

        verify(haciendaApi).consultarComprobante(anyString(), anyLong());
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.ENVIO_INCIERTO);
        assertThat(resultado.mensaje()).contains("a partir de las 10:15");
    }

    @Test
    @DisplayName("15 min después del envío: 404 → NO_TRANSMITIDO")
    void a15Minutos_404_noTransmitido() {
        ahora(ENVIO.plusMinutes(15));

        service.consultarEstadoManual(5L);

        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.NO_TRANSMITIDO);
    }

    @Test
    @DisplayName("un comprobante ACEPTADO no se consulta a mano")
    void aceptado_noConsultable() {
        comprobante.setEstado(EstadoComprobante.ACEPTADO);
        ahora(ENVIO.plusHours(2));

        assertThatThrownBy(() -> service.consultarEstadoManual(5L))
                .isInstanceOf(ConsultaManualNoPermitidaException.class);
        verify(haciendaApi, never()).consultarComprobante(anyString(), anyLong());
    }
}
