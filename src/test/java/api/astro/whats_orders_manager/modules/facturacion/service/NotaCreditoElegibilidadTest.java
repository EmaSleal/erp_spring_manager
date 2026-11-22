package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.ModalidadNota;
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
        new NotaCreditoElegibilidad(comprobantes, notas, permisos, mock(AmbienteComprobanteGuard.class),
            mock(NotaCreditoTopes.class));

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
    @DisplayName("T-046: mes de la factura original = mes de emisión de su comprobante ACEPTADO vigente")
    void periodoFacturaOriginal() {
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.of(
            ComprobanteElectronico.builder().id(1L).estado(EstadoComprobante.ACEPTADO).vigente(true)
                .fechaEmision(java.time.LocalDateTime.of(2026, 9, 29, 23, 40)).build()));
        assertThat(elegibilidad.periodoFacturaOriginal(factura(false))).contains("2026-09");

        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.of(
            ComprobanteElectronico.builder().id(1L).estado(EstadoComprobante.RECHAZADO).vigente(true)
                .fechaEmision(java.time.LocalDateTime.of(2026, 9, 29, 23, 40)).build()));
        assertThat(elegibilidad.periodoFacturaOriginal(factura(false))).isEmpty();
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
    @DisplayName("con una nota en trámite → no")
    void conNotaEnTramite() {
        when(notas.existsByFactura_IdFacturaAndEstadoIn(7, java.util.EnumSet.of(EstadoNotaFactura.EN_TRAMITE)))
            .thenReturn(true);
        assertThat(elegibilidad.evaluar(factura(false), "gerente"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("T-047: con una nota total aplicada (no parcial) → no")
    void conNotaTotalAplicada() {
        when(notas.existsByFactura_IdFacturaAndEstadoInAndModalidad(7,
            java.util.EnumSet.of(EstadoNotaFactura.APLICADA), ModalidadNota.TOTAL)).thenReturn(true);
        assertThat(elegibilidad.evaluar(factura(false), "gerente"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("T-047: con notas parciales aplicadas o en trámite solo procede otra parcial (la total, con motivo)")
    void conNotasParciales() {
        when(notas.existsByFactura_IdFacturaAndEstadoInAndModalidad(7, NotaCreditoTopes.CUENTAN,
            ModalidadNota.PARCIAL)).thenReturn(true);
        NotaCreditoElegibilidad.Resultado r = elegibilidad.evaluar(factura(false), "gerente");
        assertThat(r.puedeEmitir()).isTrue();
        assertThat(r.puedeParcial()).isTrue();
        assertThat(r.puedeTotal()).isFalse();
        assertThat(r.motivoTotal()).isEqualTo("La factura tiene notas parciales; acredite el resto con una nota parcial");
    }

    @Test
    @DisplayName("T-047: factura acreditada totalmente → no admite ninguna nota")
    void acreditadaTotalmente() {
        Factura f = factura(false);
        f.setAcreditadaTotalmente(true);
        assertThat(elegibilidad.evaluar(f, "gerente")).isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("sin FACTURA_ANULAR → no, aunque haya pagos: no se revela el motivo")
    void sinPermiso() {
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of(new Pago()));
        assertThat(elegibilidad.evaluar(factura(false), "vendedor"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, null));
    }

    @Test
    @DisplayName("T-048: con pagos aplicados la nota se ofrece (el excedente irá a saldo a favor)")
    void conPagos() {
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of(new Pago()));
        NotaCreditoElegibilidad.Resultado r = elegibilidad.evaluar(factura(false), "gerente");
        assertThat(r.puedeEmitir()).isTrue();
        assertThat(r.motivoNoDisponible()).isNull();
    }

    @Test
    @DisplayName("con pagos pero anulada o sin comprobante aceptado → sin motivo (no hay botón)")
    void conPagosPeroNoElegible() {
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of(new Pago()));
        assertThat(elegibilidad.evaluar(factura(true), "gerente").motivoNoDisponible()).isNull();
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.empty());
        assertThat(elegibilidad.evaluar(factura(false), "gerente").motivoNoDisponible()).isNull();
    }

    @Test
    @DisplayName("T-051: una 06 parcial aplicada se comporta como una 02 (admite otra parcial, bloquea la total); una 06 total aplicada bloquea todo")
    void devolucionMercanciaSegunModalidad() {
        // 06 parcial: la consulta de totales aplicadas no la ve y la de parciales sí
        when(notas.existsByFactura_IdFacturaAndEstadoInAndModalidad(7, NotaCreditoTopes.CUENTAN,
            ModalidadNota.PARCIAL)).thenReturn(true);
        NotaCreditoElegibilidad.Resultado parcial = elegibilidad.evaluar(factura(false), "gerente");
        assertThat(parcial.puedeParcial()).isTrue();
        assertThat(parcial.puedeTotal()).isFalse();

        // 06 total aplicada: la consulta de totales aplicadas la ve
        when(notas.existsByFactura_IdFacturaAndEstadoInAndModalidad(7,
            java.util.EnumSet.of(EstadoNotaFactura.APLICADA), ModalidadNota.TOTAL)).thenReturn(true);
        assertThat(elegibilidad.evaluar(factura(false), "gerente").puedeEmitir()).isFalse();
    }
}
