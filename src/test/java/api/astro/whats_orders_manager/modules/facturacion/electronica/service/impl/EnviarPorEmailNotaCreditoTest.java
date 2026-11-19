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
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.shared.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** T-042 (criterio 12): el correo del comprobante de una nota ACEPTADA adjunta el PDF de la nota. */
@DisplayName("T-042 — enviarPorEmail de una nota de crédito")
class EnviarPorEmailNotaCreditoTest {

    private static final String CLAVE = "5061NOTA";

    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final FacturaPdfService pdf = mock(FacturaPdfService.class);
    private final EmailService email = mock(EmailService.class);

    @Test
    @DisplayName("nota ACEPTADA: el cliente sale de la factura de la nota y se adjunta el PDF de la nota, no el de la factura")
    @SuppressWarnings("unchecked")
    void aceptada() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(comprobanteDeNota(EstadoComprobante.ACEPTADO)));
        when(pdf.generarPdfNotaCredito(5L)).thenReturn(new byte[] {1, 2});

        assertThat(servicio().enviarPorEmail(1L, null)).isTrue();

        verify(pdf).generarPdfNotaCredito(5L);
        verify(pdf, never()).generarPdfFactura(anyInt());
        ArgumentCaptor<Map<String, byte[]>> adjuntos = ArgumentCaptor.forClass(Map.class);
        verify(email).enviarEmailConMultiplesAdjuntos(eq("c@example.com"), anyString(), anyString(),
                adjuntos.capture());
        assertThat(adjuntos.getValue()).containsKey(CLAVE + ".pdf");
        assertThat(adjuntos.getValue().get(CLAVE + ".pdf")).containsExactly(1, 2);
    }

    @ParameterizedTest(name = "{0} → 409 fijo, sin PDF ni correo")
    @EnumSource(value = EstadoComprobante.class, names = "ACEPTADO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("nota fuera de ACEPTADO → EmisionNoPermitidaException")
    void noAceptada(EstadoComprobante estado) {
        when(repository.findById(1L)).thenReturn(Optional.of(comprobanteDeNota(estado)));

        assertThatThrownBy(() -> servicio().enviarPorEmail(1L, null))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(ComprobanteElectronicoServiceImpl.MENSAJE_EMAIL_SOLO_ACEPTADO);
        verifyNoInteractions(pdf, email);
    }

    private ComprobanteElectronicoServiceImpl servicio() {
        return new ComprobanteElectronicoServiceImpl(repository, null, null, null, null, null, null, null, null,
                mock(ComprobanteElectronicoMapper.class), pdf, email, Clock.systemUTC(), null, null, null, null,
                CatalogoUbicacionDePrueba.validador());
    }

    private static ComprobanteElectronico comprobanteDeNota(EstadoComprobante estado) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente");
        cliente.setEmail("c@example.com");
        Factura factura = new Factura();
        factura.setIdFactura(7);
        factura.setNumeroFactura("FA01-00007");
        factura.setCliente(cliente);
        NotaFactura nota = NotaFactura.builder().id(5L).factura(factura).moneda("CRC")
                .total(new BigDecimal("113.00")).build();
        // Un comprobante de nota no tiene factura propia (CHECK de V39)
        return ComprobanteElectronico.builder().id(1L).notaFactura(nota).estado(estado)
                .tipoComprobante(TipoComprobanteElectronico.NOTA_CREDITO).claveNumerica(CLAVE)
                .consecutivo("00100001030000000002").fechaEmision(LocalDateTime.of(2026, 11, 13, 10, 0))
                .empresa(new ConfiguracionEmpresa()).build();
    }
}
