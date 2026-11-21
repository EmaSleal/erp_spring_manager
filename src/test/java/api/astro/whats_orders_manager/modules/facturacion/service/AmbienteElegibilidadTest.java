package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-044 (criterio 5): nota de crédito y sustituta solo sobre una factura del ambiente activo. */
@DisplayName("T-044 — Elegibilidad por ambiente (nota de crédito y sustituta)")
class AmbienteElegibilidadTest {

    private static final String MOTIVO = "La factura se emitió en el ambiente SANDBOX; la configuración activa es PRODUCCION";

    private final ComprobanteElectronicoRepository comprobantes = mock(ComprobanteElectronicoRepository.class);
    private final NotaFacturaRepository notas = mock(NotaFacturaRepository.class);
    private final PagoService pagos = mock(PagoService.class);
    private final PermisoService permisos = mock(PermisoService.class);
    private final AmbienteComprobanteGuard guard = mock(AmbienteComprobanteGuard.class);
    private final NotaCreditoElegibilidad notaCredito =
        new NotaCreditoElegibilidad(comprobantes, notas, permisos, guard, mock(NotaCreditoTopes.class));
    private final ComprobanteElectronico aceptado = ComprobanteElectronico.builder().id(1L)
        .estado(EstadoComprobante.ACEPTADO).vigente(true).ambiente(AmbienteHacienda.SANDBOX).build();

    @BeforeEach
    void setUp() {
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.of(aceptado));
        when(notas.existsByFactura_IdFacturaAndEstadoIn(anyInt(), any())).thenReturn(false);
        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of());
        when(permisos.tienePermisoPorCodigo("gerente", "FACTURA_ANULAR")).thenReturn(true);
        when(guard.motivoReferencia(aceptado)).thenReturn(Optional.of(MOTIVO));
    }

    private static Factura factura() {
        Factura f = new Factura();
        f.setIdFactura(7);
        f.setAnulada(false);
        return f;
    }

    @Test
    @DisplayName("nota: factura de otro ambiente → false con el motivo fijo")
    void notaOtroAmbiente() {
        assertThat(notaCredito.evaluar(factura(), "gerente"))
            .isEqualTo(new NotaCreditoElegibilidad.Resultado(false, MOTIVO));
    }

    @Test
    @DisplayName("nota, orden: sin permiso o sin ACEPTADO no muestra el motivo de ambiente; el ambiente gana a los pagos")
    void notaPrecedencia() {
        when(permisos.tienePermisoPorCodigo("gerente", "FACTURA_ANULAR")).thenReturn(false);
        assertThat(notaCredito.evaluar(factura(), "gerente").motivoNoDisponible()).isNull();
        when(permisos.tienePermisoPorCodigo("gerente", "FACTURA_ANULAR")).thenReturn(true);

        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.of(
            ComprobanteElectronico.builder().id(2L).estado(EstadoComprobante.ENVIADO).vigente(true).build()));
        assertThat(notaCredito.evaluar(factura(), "gerente").motivoNoDisponible()).isNull();
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(7)).thenReturn(Optional.of(aceptado));

        when(pagos.findPagosValidosByFacturaId(7)).thenReturn(List.of(new api.astro.whats_orders_manager.modules.facturacion.model.Pago()));
        assertThat(notaCredito.evaluar(factura(), "gerente").motivoNoDisponible()).isEqualTo(MOTIVO);
    }

    @Test
    @DisplayName("nota: mismo ambiente → sigue habilitada")
    void notaMismoAmbiente() {
        when(guard.motivoReferencia(aceptado)).thenReturn(Optional.empty());
        assertThat(notaCredito.evaluar(factura(), "gerente")).isEqualTo(new NotaCreditoElegibilidad.Resultado(true, null));
    }

    @Test
    @DisplayName("emitirNotaCreditoTotal: factura de otro ambiente → 409 con el motivo, sin nota ni comprobante")
    void emitirNotaConflicto() {
        FacturaRepository facturas = mock(FacturaRepository.class);
        NotaFacturaRepository notasRepo = mock(NotaFacturaRepository.class);
        var comprobanteService = mock(api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService.class);
        Factura f = factura();
        f.setNumeroFactura("FA-1");
        when(facturas.findByIdParaActualizar(7)).thenReturn(Optional.of(f));
        NotaFacturaService servicio = new NotaFacturaService(facturas, notasRepo, comprobantes, comprobanteService, mock(api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService.class),
            new api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService(null),
            java.time.Clock.systemUTC(), guard, new AtribucionFiscalService("ORIGINAL"), mock(NotaCreditoTopes.class));

        assertThatThrownBy(() -> servicio.emitirNotaCreditoTotal(7,
            api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura.CREDITO, "01", "Anulación", "gerente"))
            .isInstanceOf(NotaFacturaConflictoException.class).hasMessage(MOTIVO);
        verify(notasRepo, never()).saveAndFlush(any());
        verify(comprobanteService, never()).generarYFirmarDesdeNota(any());
    }

    @Test
    @DisplayName("sustituta: la bandera es false y el motivo sale de la regla de ambiente; evaluar no cambia")
    void sustitutaBandera() {
        NotaFacturaRepository notasRepo = mock(NotaFacturaRepository.class);
        FacturaRepository facturas = mock(FacturaRepository.class);
        SustitucionElegibilidad regla = new SustitucionElegibilidad(notasRepo, comprobantes, facturas, guard);
        var nota = api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura.builder().id(9L)
            .tipo(api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura.CREDITO)
            .estado(EstadoNotaFactura.APLICADA).codigoReferencia("01").comprobanteReferencia(aceptado).build();
        aceptado.setClaveNumerica("506ORIGEN");
        aceptado.setFechaEmision(java.time.LocalDateTime.of(2026, 11, 1, 10, 0));
        Factura original = factura();
        original.setAnulada(true);
        when(notasRepo.findByFactura_IdFacturaOrderByIdAsc(7)).thenReturn(List.of(nota));
        when(comprobantes.findFirstByNotaFactura_IdAndVigenteTrue(9L)).thenReturn(Optional.of(
            ComprobanteElectronico.builder().id(3L).estado(EstadoComprobante.ACEPTADO).build()));
        when(facturas.findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(7)).thenReturn(Optional.empty());

        assertThat(regla.evaluar(original).elegible()).isTrue(); // sin la regla de ambiente: PDF y contabilidad
        assertThat(regla.evaluarSinSustituta(original).elegible()).isTrue();
        assertThat(regla.puedeEmitirSustituta(original)).isFalse();
        assertThat(regla.motivoSustitutaNoDisponible(original)).contains(MOTIVO);

        when(guard.motivoReferencia(aceptado)).thenReturn(Optional.empty());
        assertThat(regla.puedeEmitirSustituta(original)).isTrue();
        assertThat(regla.motivoSustitutaNoDisponible(original)).isEmpty();
    }
}
