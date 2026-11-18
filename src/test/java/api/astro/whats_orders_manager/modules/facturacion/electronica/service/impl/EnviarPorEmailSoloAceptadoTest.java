package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.shared.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** T-039 (criterio 13, ADR-045 §6): el correo manual del comprobante solo sale con el comprobante ACEPTADO. */
@DisplayName("T-039 — enviarPorEmail solo con comprobante ACEPTADO")
class EnviarPorEmailSoloAceptadoTest {

    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final FacturaPdfService pdf = mock(FacturaPdfService.class);
    private final EmailService email = mock(EmailService.class);

    @ParameterizedTest(name = "{0} → 409 fijo, sin PDF ni correo")
    @EnumSource(value = EstadoComprobante.class, names = "ACEPTADO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("en cualquier estado distinto de ACEPTADO lanza EmisionNoPermitidaException antes de hacer nada")
    void noAceptado(EstadoComprobante estado) {
        when(repository.findById(1L)).thenReturn(Optional.of(comprobante(estado)));

        assertThatThrownBy(() -> servicio().enviarPorEmail(1L, null))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(ComprobanteElectronicoServiceImpl.MENSAJE_EMAIL_SOLO_ACEPTADO);
        verifyNoInteractions(pdf, email);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ACEPTADO adjunta el PDF y envía")
    void aceptado() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(comprobante(EstadoComprobante.ACEPTADO)));
        when(pdf.generarPdfFactura(7)).thenReturn(new byte[] {1});

        assertThat(servicio().enviarPorEmail(1L, null)).isTrue();

        verify(pdf).generarPdfFactura(7);
        verify(email).enviarEmailConMultiplesAdjuntos(anyString(), anyString(), anyString(), any(Map.class));
    }

    private ComprobanteElectronicoServiceImpl servicio() {
        return new ComprobanteElectronicoServiceImpl(repository, null, null, null, null, null, null, null, null,
                mock(ComprobanteElectronicoMapper.class), pdf, email, Clock.systemUTC(), null, null, null, null,
                CatalogoUbicacionDePrueba.validador());
    }

    private static ComprobanteElectronico comprobante(EstadoComprobante estado) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente");
        cliente.setEmail("c@example.com");
        Factura factura = new Factura();
        factura.setIdFactura(7);
        factura.setNumeroFactura("FA01-00007");
        factura.setCliente(cliente);
        return ComprobanteElectronico.builder().id(1L).factura(factura).estado(estado)
                .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA).claveNumerica("5061")
                .consecutivo("001").fechaEmision(LocalDateTime.of(2026, 11, 13, 10, 0))
                .empresa(new ConfiguracionEmpresa()).build();
    }
}
