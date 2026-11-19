package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-042 (criterios 1 y 2): la bandera «Emitir nota de crédito» sale del servidor. */
@DisplayName("T-042 — Elegibilidad de «Emitir nota de crédito»")
class NotaCreditoElegibilidadTest {

    private final ComprobanteElectronicoRepository comprobantes = mock(ComprobanteElectronicoRepository.class);
    private final NotaFacturaRepository notas = mock(NotaFacturaRepository.class);
    private final PagoService pagos = mock(PagoService.class);
    private final PermisoService permisos = mock(PermisoService.class);
    private final NotaCreditoElegibilidad elegibilidad =
        new NotaCreditoElegibilidad(comprobantes, notas, pagos, permisos);

    @BeforeEach
    void setUp() {
        // Caso base: aceptada, no anulada, sin nota, sin pagos, con permiso
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.of(
            ComprobanteElectronico.builder().id(1L).estado(EstadoComprobante.ACEPTADO).vigente(true).build()));
        when(notas.existsByFactura_IdFacturaAndEstadoIn(anyInt(), any())).thenReturn(false);
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of());
        when(permisos.tienePermisoPorCodigo("gerente", "FACTURA_ANULAR")).thenReturn(true);
    }

    private static Factura factura(boolean anulada) {
        Factura f = new Factura();
        f.setIdFactura(7);
        f.setAnulada(anulada);
        return f;
    }

    @Test
    @DisplayName("comprobante ACEPTADO, sin nota, sin pagos, con permiso → se puede emitir")
    void aceptada() {
        assertThat(elegibilidad.evaluar(factura(false), "gerente"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(true, null));
    }

    @Test
    @DisplayName("sin comprobante vigente → no")
    void sinComprobante() {
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.empty());
        assertThat(elegibilidad.evaluar(factura(false), "gerente"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("comprobante en otro estado → no")
    void comprobanteNoAceptado() {
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.of(
            ComprobanteElectronico.builder().id(1L).estado(EstadoComprobante.ENVIADO).vigente(true).build()));
        assertThat(elegibilidad.evaluar(factura(false), "gerente").puedeEmitir()).isFalse();
    }

    @Test
    @DisplayName("factura anulada → no")
    void anulada() {
        assertThat(elegibilidad.evaluar(factura(true), "gerente"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("con una nota en trámite (o aplicada) → no")
    void conNotaEnTramite() {
        when(notas.existsByFactura_IdFacturaAndEstadoIn(7,
            java.util.EnumSet.of(EstadoNotaFactura.EN_TRAMITE, EstadoNotaFactura.APLICADA))).thenReturn(true);
        assertThat(elegibilidad.evaluar(factura(false), "gerente"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("sin FACTURA_ANULAR → no, aunque haya pagos: no se revela el motivo")
    void sinPermiso() {
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of(new Pago()));
        assertThat(elegibilidad.evaluar(factura(false), "vendedor"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("con pagos aplicados → botón deshabilitado con el motivo fijo")
    void conPagos() {
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of(new Pago()));
        NotaCreditoElegibilidad.Resultado r = elegibilidad.evaluar(factura(false), "gerente");
        assertThat(r.puedeEmitir()).isFalse();
        assertThat(r.motivoNoDisponible())
            .isEqualTo("La nota de crédito sobre facturas con pagos llega en una próxima versión");
    }

    @Test
    @DisplayName("con pagos pero anulada o sin comprobante aceptado → sin motivo (no hay botón)")
    void conPagosPeroNoElegible() {
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of(new Pago()));
        assertThat(elegibilidad.evaluar(factura(true), "gerente").motivoNoDisponible()).isNull();
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.empty());
        assertThat(elegibilidad.evaluar(factura(false), "gerente").motivoNoDisponible()).isNull();
    }
}
