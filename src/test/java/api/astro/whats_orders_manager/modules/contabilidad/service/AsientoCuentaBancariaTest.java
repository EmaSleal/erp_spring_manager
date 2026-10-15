package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-015 (ADR-018, ADR-014, ADR-017) — reglas del asiento cuando el medio de pago es bancario:
 * sin cuenta bancaria no hay respaldo por clave (IllegalStateException con el número del pago), y la
 * hoja de la cuenta bancaria debe ser una hoja activa que acepta movimientos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("T-015 — asientos de cobros y pagos a proveedor con cuenta bancaria")
class AsientoCuentaBancariaTest {

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private AsientoContableService service;
    private CuentaContable hojaBanco;

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);
        long id = 1;
        for (String clave : List.of("VENTA_CXC_CLIENTES", "COBRO_ANTICIPOS_CLIENTES", "COBRO_CAJA_EFECTIVO",
                "CPP_CUENTA_POR_PAGAR", "CPP_BANCO_EFECTIVO")) {
            ParametroContable p = new ParametroContable();
            p.setClave(clave);
            p.setCuentaContable(cuenta(id++, clave));
            when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(p));
        }
        hojaBanco = cuenta(50L, "1.1.01.002.001");
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("cobro TRANSFERENCIA sin cuenta bancaria: IllegalStateException con el número del pago, sin respaldo por clave")
    void cobroSinCuentaBancaria_lanzaIllegalState() {
        Pago pago = cobro(MetodoPago.TRANSFERENCIA, null);

        assertThatThrownBy(() -> service.generarAsientoPago(pago))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAG-20261001-0001")
                .hasMessageContaining("cuenta bancaria de destino");
        verify(parametroContableRepository, never()).findByClave("COBRO_CAJA_TRANSFERENCIA");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("pago a proveedor SINPE sin cuenta bancaria: IllegalStateException con el número del pago, sin respaldo por clave")
    void pagoProveedorSinCuentaBancaria_lanzaIllegalState() {
        PagoProveedor pago = pagoProveedor(MetodoPagoProveedor.SINPE, null);

        assertThatThrownBy(() -> service.generarAsientoPagoProveedor(pago))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PP-2026-00001")
                .hasMessageContaining("cuenta bancaria de origen");
        verify(parametroContableRepository, never()).findByClave("CPP_BANCO_SINPE");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("hoja de la cuenta bancaria inactiva: IllegalStateException (ADR-017), sin asiento")
    void hojaInactiva_lanzaIllegalState() {
        hojaBanco.setActiva(false);

        assertThatThrownBy(() -> service.generarAsientoPago(cobro(MetodoPago.TRANSFERENCIA, banco())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1.1.01.002.001").hasMessageContaining("inactiva");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("hoja de la cuenta bancaria con subcuentas (agrupadora): IllegalStateException (ADR-017), sin asiento")
    void hojaAgrupadora_lanzaIllegalState() {
        when(cuentaRepository.countSubcuentas(50L)).thenReturn(1L);

        assertThatThrownBy(() -> service.generarAsientoPagoProveedor(pagoProveedor(MetodoPagoProveedor.CHEQUE, banco())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1.1.01.002.001").hasMessageContaining("agrupadora");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cobro EFECTIVO con una cuenta bancaria cargada igual usa COBRO_CAJA_EFECTIVO")
    void cobroEfectivo_usaClaveDeCaja() {
        AsientoContable asiento = service.generarAsientoPago(cobro(MetodoPago.EFECTIVO, banco()));

        assertThat(asiento.getDetalles().get(0).getCuenta().getCodigo()).isEqualTo("COBRO_CAJA_EFECTIVO");
        assertThat(asiento.getDetalles()).noneMatch(d -> d.getCuenta() == hojaBanco);
    }

    @Test
    @DisplayName("cada medio bancario de pago a proveedor acredita la hoja de su cuenta bancaria")
    void mediosBancariosProveedor_acreditanHoja() {
        for (MetodoPagoProveedor metodo : MetodoPagoProveedor.values()) {
            AsientoContable asiento = service.generarAsientoPagoProveedor(pagoProveedor(metodo,
                    metodo.requiereCuentaBancaria() ? banco() : null));
            CuentaContable credito = asiento.getDetalles().get(1).getCuenta();
            assertThat(credito.getCodigo()).as(metodo.name())
                    .isEqualTo(metodo.requiereCuentaBancaria() ? "1.1.01.002.001" : "CPP_BANCO_EFECTIVO");
            assertThat(asiento.getDetalles().get(1).getHaber()).isEqualByComparingTo("5000.00");
        }
    }

    @Test
    @DisplayName("contrapartida de CxP manual: excluye las hojas de las cuentas bancarias (no tienen clave)")
    void contrapartida_excluyeHojasBancarias() {
        CuentaContable gasto = cuenta(60L, "5.1.03.001");
        gasto.setTipo(TipoCuenta.EGRESO);
        hojaBanco.setTipo(TipoCuenta.ACTIVO);
        when(cuentaRepository.findByAceptaMovimientosAndActiva(true, true)).thenReturn(List.of(gasto, hojaBanco));
        when(cuentaRepository.findCuentasDeCuentasBancarias())
                .thenReturn(List.<Object[]>of(new Object[] {50L, "Banco Nacional - Colones"}));
        when(cuentaRepository.findById(50L)).thenReturn(Optional.of(hojaBanco));

        assertThat(service.obtenerCuentasContrapartidaCxpManual()).containsExactly(gasto);
        assertThatThrownBy(() -> service.validarContrapartidaCxpManual(50L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuenta bancaria Banco Nacional - Colones");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CuentaBancaria banco() {
        CuentaBancaria banco = new CuentaBancaria();
        banco.setIdCuentaBancaria(1);
        banco.setEntidad("Banco Nacional - Colones");
        banco.setMoneda("CRC");
        banco.setActiva(true);
        banco.setCuentaContable(hojaBanco);
        return banco;
    }

    private Pago cobro(MetodoPago metodo, CuentaBancaria destino) {
        Pago pago = new Pago();
        pago.setIdPago(9L);
        pago.setNumeroPago("PAG-20261001-0001");
        pago.setMonto(new BigDecimal("5000.00"));
        pago.setMoneda(MonedaFE.CRC);
        pago.setTipoCambio(BigDecimal.ONE);
        pago.setMetodoPago(metodo);
        pago.setEstado(EstadoPago.CONFIRMADO);
        pago.setFechaPago(LocalDateTime.of(2026, 10, 1, 9, 0));
        pago.setCuentaBancariaDestino(destino);
        return pago;
    }

    private PagoProveedor pagoProveedor(MetodoPagoProveedor metodo, CuentaBancaria origen) {
        CuentaPorPagar cpp = CuentaPorPagar.builder().id(10L).numero("CPP-2026-00001")
                .monto(new BigDecimal("5000.00")).saldoPendiente(BigDecimal.ZERO)
                .moneda(MonedaFE.CRC).tipoCambio(BigDecimal.ONE).estado(EstadoCuentaPorPagar.PAGADA).build();
        return PagoProveedor.builder().id(20L).numero("PP-2026-00001").cuentaPorPagar(cpp)
                .monto(new BigDecimal("5000.00")).moneda(MonedaFE.CRC).tipoCambio(BigDecimal.ONE)
                .metodoPago(metodo).cuentaBancariaOrigen(origen).fecha(LocalDate.of(2026, 10, 2)).build();
    }

    private static CuentaContable cuenta(long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre(codigo);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return c;
    }
}
