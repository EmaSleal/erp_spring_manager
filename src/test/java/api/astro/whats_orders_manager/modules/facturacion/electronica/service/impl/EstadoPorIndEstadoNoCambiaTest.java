package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-036 (criterio 9): el estado del comprobante lo decide ind-estado del JSON (T-033), no el enum MensajeHacienda.
 * La corrección de los códigos del enum no lo cambia.
 */
@DisplayName("T-036 — el estado sigue decidido por ind-estado")
class EstadoPorIndEstadoNoCambiaTest {

    private static String mensajeXml(int mensaje, String estado) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><MensajeHacienda xmlns=\"https://cdn.comprobanteselectronicos"
                + ".go.cr/xml-schemas/v4.4/mensajeHacienda\"><Clave>1</Clave><Mensaje>" + mensaje + "</Mensaje>"
                + "<EstadoMensaje>" + estado + "</EstadoMensaje><DetalleMensaje>detalle de prueba</DetalleMensaje>"
                + "</MensajeHacienda>";
    }

    private ComprobanteElectronico consultar(String indEstado, MensajeHacienda enumDelDto, int mensajeXml, String estadoXml) {
        ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
        ConfiguracionHaciendaService configuracion = mock(ConfiguracionHaciendaService.class);
        HaciendaApiService hacienda = mock(HaciendaApiService.class);
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder().idConfiguracion(1).build();
        ComprobanteElectronico comprobante = ComprobanteElectronico.builder().id(3L).empresa(empresa)
                .claveNumerica("50613112600011820087800100001010000000028172833386")
                .estado(EstadoComprobante.ENVIADO).build();
        when(repository.findById(3L)).thenReturn(Optional.of(comprobante));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configuracion.obtenerConfiguracionActiva())
                .thenReturn(ConfiguracionHacienda.builder().id(9L).empresa(empresa).build());
        when(hacienda.consultarComprobante(anyString(), anyLong())).thenReturn(RespuestaHaciendaDTO.builder()
                .indicadorEstado(indEstado).codigoMensaje(enumDelDto).mensaje("")
                .xmlRespuesta(Base64.getEncoder().encodeToString(
                        mensajeXml(mensajeXml, estadoXml).getBytes(StandardCharsets.UTF_8)))
                .codigoHttp(200).build());
        new ComprobanteElectronicoServiceImpl(repository, null, null, configuracion, null, null, null, hacienda, null,
                mock(ComprobanteElectronicoMapper.class), null, null, Clock.systemUTC(), null, null, null, (c, t) -> c.siguienteConsecutivo(t),
            CatalogoUbicacionDePrueba.validador())
                .consultarYActualizarEstado(3L);
        return comprobante;
    }

    @Test
    @DisplayName("ind-estado 'rechazado' con Mensaje = 3 queda RECHAZADO")
    void rechazado() {
        ComprobanteElectronico c = consultar("rechazado", MensajeHacienda.RECHAZADO, 3, "Rechazado");
        assertThat(c.getEstado()).isEqualTo(EstadoComprobante.RECHAZADO);
        assertThat(c.getCodigoRespuesta()).isEqualTo("3");
    }

    @Test
    @DisplayName("ind-estado 'aceptado' con Mensaje = 1 queda ACEPTADO")
    void aceptado() {
        ComprobanteElectronico c = consultar("aceptado", MensajeHacienda.ACEPTADO, 1, "Aceptado");
        assertThat(c.getEstado()).isEqualTo(EstadoComprobante.ACEPTADO);
        assertThat(c.getCodigoRespuesta()).isEqualTo("1");
    }
}
