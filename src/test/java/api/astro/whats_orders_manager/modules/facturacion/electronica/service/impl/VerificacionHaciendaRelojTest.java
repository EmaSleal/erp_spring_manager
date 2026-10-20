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
 * T-025 (ADR-030) — "verificar con Hacienda" un ENVIO_INCIERTO solo 15 minutos después del último envío, con un
 * reloj inyectado fijo. El cliente de Hacienda es un mock: ninguna llamada real.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("T-025 — verificación con Hacienda: regla de 15 minutos con reloj inyectado")
class VerificacionHaciendaRelojTest {

    private static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");
    private static final LocalDateTime ENVIO = LocalDateTime.of(2026, 10, 17, 10, 0);

    @Mock private ComprobanteElectronicoRepository repository;
    @Mock private ConfiguracionHaciendaService configuracionService;
    @Mock private HaciendaApiService haciendaApi;
    @Mock private ComprobanteElectronicoMapper mapper;
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
    }

    private void ahora(LocalDateTime momento) {
        when(clock.instant()).thenReturn(momento.atZone(ZONA).toInstant());
    }

    @Test
    @DisplayName("14 min 59 s después del envío → rechazado con la hora desde la que se puede (10:15), sin consultar")
    void antesDe15Minutos() {
        ahora(ENVIO.plusMinutes(14).plusSeconds(59));

        assertThatThrownBy(() -> service.verificarConHacienda(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a partir de las 10:15");
        verify(haciendaApi, never()).consultarComprobante(anyString(), anyLong());
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.ENVIO_INCIERTO);
    }

    @Test
    @DisplayName("15 min después del envío → consulta; 404 → NO_TRANSMITIDO")
    void a15Minutos_404() {
        ahora(ENVIO.plusMinutes(15));
        when(haciendaApi.consultarComprobante(anyString(), anyLong()))
                .thenReturn(RespuestaHaciendaDTO.builder().documentoNoExiste(true).codigoHttp(404).build());

        service.verificarConHacienda(5L);

        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.NO_TRANSMITIDO);
    }

    @Test
    @DisplayName("un comprobante que no está en ENVIO_INCIERTO no se verifica")
    void soloEnvioIncierto() {
        comprobante.setEstado(EstadoComprobante.ACEPTADO);
        ahora(ENVIO.plusHours(2));

        assertThatThrownBy(() -> service.verificarConHacienda(5L)).isInstanceOf(IllegalStateException.class);
        verify(haciendaApi, never()).consultarComprobante(anyString(), anyLong());
    }
}
