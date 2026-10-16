package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-020 (ADR-023 + aclaración aprobada 2026-10-15) — reglas de AsientoContableService.revertirManual:
 * se revierte un asiento CONTABILIZADO de tipo no automático y sin documento de origen; con
 * documento, el rechazo nombra el documento; AUTOMATICO_* sin documento es una inconsistencia.
 * El motivo es obligatorio. Nada pasa a ANULADO.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-020 — AsientoContableService.revertirManual: reglas")
class AsientoRevertirManualTest {

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private AsientoContableService service;

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);
        lenient().when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(i -> i.getArgument(0));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = TipoAsiento.class, names = {"MANUAL", "AJUSTE", "APERTURA", "CIERRE", "INGRESO"})
    @DisplayName("tipos no automáticos, contabilizados y sin documento: se revierten; el original queda CONTABILIZADO")
    void noAutomaticoSinDocumento_seRevierte(TipoAsiento tipo) {
        AsientoContable original = contabilizado(10L, tipo);

        AsientoContable reversion = service.revertirManual(10L, "  Error de registro  ");

        assertThat(reversion.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(reversion.getAsientoRevertido()).isSameAs(original);
        assertThat(reversion.getFecha()).isEqualTo(LocalDate.now());
        assertThat(reversion.getConcepto()).endsWith(" - Error de registro");
        assertThat(original.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
    }

    @ParameterizedTest(name = "motivo [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("motivo nulo, vacío o en blanco: se rechaza sin buscar ni guardar")
    void motivoObligatorio(String motivo) {
        assertThatThrownBy(() -> service.revertirManual(10L, motivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El motivo de la reversión es obligatorio");
        verify(asientoRepository, never()).findById(any());
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("BORRADOR: se rechaza (no afectó saldos)")
    void borrador_rechazo() {
        AsientoContable borrador = contabilizado(10L, TipoAsiento.MANUAL);
        borrador.setEstado(EstadoAsiento.BORRADOR);

        assertThatThrownBy(() -> service.revertirManual(10L, "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se revierten asientos contabilizados");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("AUTOMATICO_VENTA con factura: el rechazo nombra la factura e indica anularla")
    void conFactura_rechazoNombraFactura() {
        AsientoContable asiento = contabilizado(10L, TipoAsiento.AUTOMATICO_VENTA);
        Factura factura = new Factura();
        factura.setNumeroFactura("FA01-00007");
        asiento.setFactura(factura);

        assertThatThrownBy(() -> service.revertirManual(10L, "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Este asiento pertenece a la factura FA01-00007; anule la factura para revertirlo");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("con documento de origen, sea cual sea el tipo (MANUAL con CxP): rechazo que nombra el documento")
    void manualConDocumento_rechazo() {
        AsientoContable asiento = contabilizado(10L, TipoAsiento.MANUAL);
        CuentaPorPagar cpp = new CuentaPorPagar();
        cpp.setNumero("CPP-0003");
        asiento.setCuentaPorPagar(cpp);

        assertThatThrownBy(() -> service.revertirManual(10L, "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Este asiento pertenece a la cuenta por pagar CPP-0003; anule la cuenta por pagar para revertirlo");
    }

    @Test
    @DisplayName("pago, pago a proveedor y planilla: el rechazo nombra cada documento")
    void otrosDocumentos_rechazo() {
        AsientoContable conPago = contabilizado(11L, TipoAsiento.AUTOMATICO_PAGO);
        Pago pago = new Pago();
        pago.setNumeroPago("PAG-0001");
        conPago.setPago(pago);
        assertThatThrownBy(() -> service.revertirManual(11L, "motivo"))
                .hasMessage("Este asiento pertenece al pago PAG-0001; anule el pago para revertirlo");

        AsientoContable conPagoProveedor = contabilizado(12L, TipoAsiento.AUTOMATICO_GASTO);
        PagoProveedor pp = new PagoProveedor();
        pp.setNumero("PP-0002");
        conPagoProveedor.setPagoProveedor(pp);
        assertThatThrownBy(() -> service.revertirManual(12L, "motivo"))
                .hasMessage("Este asiento pertenece al pago a proveedor PP-0002; anule el pago a proveedor para revertirlo");

        AsientoContable conNomina = contabilizado(13L, TipoAsiento.AUTOMATICO_NOMINA);
        Nomina nomina = new Nomina();
        nomina.setNumero("NOM-2026-09");
        conNomina.setNomina(nomina);
        assertThatThrownBy(() -> service.revertirManual(13L, "motivo"))
                .hasMessage("Este asiento pertenece a la planilla NOM-2026-09; anule la planilla para revertirlo");
        verify(asientoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = TipoAsiento.class, names = "AUTOMATICO_.*", mode = EnumSource.Mode.MATCH_ALL)
    @DisplayName("AUTOMATICO_* sin documento: rechazo por inconsistencia")
    void automaticoSinDocumento_rechazo(TipoAsiento tipo) {
        contabilizado(10L, tipo);

        assertThatThrownBy(() -> service.revertirManual(10L, "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Asiento automático sin documento de origen; no se puede revertir desde Contabilidad");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("revertir dos veces: se rechaza")
    void yaRevertido_rechazo() {
        contabilizado(10L, TipoAsiento.MANUAL);
        when(asientoRepository.existsByAsientoRevertidoIdAsiento(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.revertirManual(10L, "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue revertido");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("revertir una reversión: se rechaza")
    void reversion_rechazo() {
        AsientoContable reversion = contabilizado(10L, TipoAsiento.MANUAL);
        reversion.setAsientoRevertido(new AsientoContable());

        assertThatThrownBy(() -> service.revertirManual(10L, "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("es una reversión y no puede revertirse");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("asiento inexistente: IllegalArgumentException")
    void inexistente_rechazo() {
        when(asientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revertirManual(99L, "motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Asiento no encontrado");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AsientoContable contabilizado(Long id, TipoAsiento tipo) {
        AsientoContable a = new AsientoContable();
        a.setIdAsiento(id);
        a.setNumero("ASI-2026-00" + id);
        a.setFecha(LocalDate.of(2026, 9, 1));
        a.setConcepto("Asiento de prueba");
        a.setTipo(tipo);
        a.setEstado(EstadoAsiento.CONTABILIZADO);
        a.setDetalles(new ArrayList<>());
        a.agregarDetalle(linea(1L, "100.00", "0"));
        a.agregarDetalle(linea(2L, "0", "100.00"));
        lenient().when(asientoRepository.findById(id)).thenReturn(Optional.of(a));
        return a;
    }

    private static DetalleAsiento linea(Long cuentaId, String debe, String haber) {
        CuentaContable cuenta = new CuentaContable();
        cuenta.setIdCuenta(cuentaId);
        cuenta.setCodigo("C" + cuentaId);
        cuenta.setTipo(TipoCuenta.ACTIVO);
        DetalleAsiento d = new DetalleAsiento();
        d.setCuenta(cuenta);
        d.setDebe(new BigDecimal(debe));
        d.setHaber(new BigDecimal(haber));
        d.setDescripcion("Línea " + cuentaId);
        return d;
    }
}
