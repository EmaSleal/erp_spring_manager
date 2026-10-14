package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.impl.CuentaPorPagarServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-007 (ADR-011) — el asiento de compra de una OC en moneda extranjera se registra en CRC
 * con el tipo de cambio de la CxP (heredado de la OC) y cuadra exactamente (criterio 8).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AsientoContableService.generarAsientoCompra — multimoneda (ADR-011)")
class AsientoCompraMultimonedaTest {

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;
    @Mock private CuentaPorPagarRepository cuentaPorPagarRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private TipoCambioRepository tipoCambioRepository;

    private AsientoContableService service;
    private final Map<String, CuentaContable> cuentas = new HashMap<>();
    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);
        long id = 1;
        for (String clave : List.of("CPP_CUENTA_COMPRAS", "CPP_IVA_CREDITO_FISCAL", "CPP_CUENTA_POR_PAGAR",
                "CPP_BANCO_EFECTIVO")) {
            CuentaContable c = new CuentaContable();
            c.setIdCuenta(id++);
            c.setCodigo(clave);
            c.setNombre(clave);
            c.setActiva(true);
            c.setAceptaMovimientos(true);
            cuentas.put(clave, c);
            ParametroContable p = new ParametroContable();
            p.setClave(clave);
            p.setCuentaContable(c);
            when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(p));
        }
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Prueba");
        proveedor.setSaldoPendiente(BigDecimal.ZERO);
        proveedor.setDiasCredito(0);
    }

    @Test
    @DisplayName("criterio 8: OC USD 100 + IVA 13 = 113 a TC 500 → DR Compras 50000 / DR IVA 6500 / CR CxP 56500")
    void compraUsd_convierteACrc() {
        CuentaPorPagar cpp = cpp(oc("100.00", "13.00", "113.00", MonedaFE.USD, "500"), MonedaFE.USD, "500");

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_COMPRA);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(debe(asiento, "CPP_CUENTA_COMPRAS")).isEqualByComparingTo("50000.00");
        assertThat(debe(asiento, "CPP_IVA_CREDITO_FISCAL")).isEqualByComparingTo("6500.00");
        assertThat(haber(asiento, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("56500.00");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
    }

    @Test
    @DisplayName("criterio 8: caso de redondeo (TC 503.27, montos con céntimos) cuadra exactamente; el centavo va a Compras")
    void compraUsd_redondeoCuadra() {
        // 10.05 × 503.27 = 5057.86 ; 1.31 × 503.27 = 659.28 ; 11.36 × 503.27 = 5717.15
        // 5057.86 + 659.28 = 5717.14 → Compras se ajusta a 5057.87
        CuentaPorPagar cpp = cpp(oc("10.05", "1.31", "11.36", MonedaFE.USD, "503.27"), MonedaFE.USD, "503.27");

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(haber(asiento, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("5717.15");
        assertThat(debe(asiento, "CPP_IVA_CREDITO_FISCAL")).isEqualByComparingTo("659.28");
        assertThat(debe(asiento, "CPP_CUENTA_COMPRAS")).isEqualByComparingTo("5057.87");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        assertThat(asiento.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("compra en CRC se registra por sus montos nominales (TC 1)")
    void compraCrc_sinConversion() {
        CuentaPorPagar cpp = cpp(oc("100000.00", "13000.00", "113000.00", MonedaFE.CRC, "1"), MonedaFE.CRC, "1");

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(debe(asiento, "CPP_CUENTA_COMPRAS")).isEqualByComparingTo("100000.00");
        assertThat(debe(asiento, "CPP_IVA_CREDITO_FISCAL")).isEqualByComparingTo("13000.00");
        assertThat(haber(asiento, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("113000.00");
    }

    @Test
    @DisplayName("criterio 18: el asiento de compra se fecha con la emisión de la CxP (2026-09-30), no con hoy")
    void compra_fechaAsientoEsFechaEmision() {
        CuentaPorPagar cpp = cpp(oc("100.00", "13.00", "113.00", MonedaFE.USD, "500"), MonedaFE.USD, "500");
        cpp.setFechaEmision(LocalDate.of(2026, 9, 30));

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(asiento.getFecha()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    @DisplayName("criterio 18: sin fecha de emisión el asiento de compra toma la fecha de recepción de la OC")
    void compra_sinFechaEmision_usaRecepcionOc() {
        OrdenCompra oc = oc("100.00", "13.00", "113.00", MonedaFE.USD, "500");
        oc.setFechaRecepcion(LocalDate.of(2026, 9, 28));
        CuentaPorPagar cpp = cpp(oc, MonedaFE.USD, "500");
        cpp.setFechaEmision(null);

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(asiento.getFecha()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    @DisplayName("ADR-014: sin fecha de emisión ni de recepción de la OC → IllegalStateException con la CxP y el campo, sin guardar")
    void compra_sinFechas_lanzaIllegalState() {
        CuentaPorPagar cpp = cpp(oc("100.00", "13.00", "113.00", MonedaFE.USD, "500"), MonedaFE.USD, "500");
        cpp.setFechaEmision(null);

        assertThatThrownBy(() -> service.generarAsientoCompra(cpp))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CPP-2026-00001")
                .hasMessageContaining("fechaEmision")
                .hasMessageContaining("fechaRecepcion");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("compra USD sin tipo de cambio falla con mensaje claro y no se guarda")
    void compraUsdSinTipoCambio_falla() {
        CuentaPorPagar cpp = cpp(oc("100.00", "13.00", "113.00", MonedaFE.USD, null), MonedaFE.USD, null);

        assertThatThrownBy(() -> service.generarAsientoCompra(cpp))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tipo de cambio");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("la CxP generada desde una OC USD hereda moneda y tipo de cambio de la OC, y su asiento queda en CRC")
    void crearDesdeOrdenCompra_heredaTipoCambio() {
        OrdenCompra oc = oc("100.00", "13.00", "113.00", MonedaFE.USD, "500");
        oc.setId(5L);
        oc.setEstado(EstadoOrdenCompra.RECIBIDA);
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc));
        when(cuentaPorPagarRepository.save(any(CuentaPorPagar.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaPorPagarRepository.findUltimoConsecutivo(anyInt())).thenReturn(null);
        CuentaPorPagarServiceImpl cxpService = new CuentaPorPagarServiceImpl(cuentaPorPagarRepository,
                proveedorRepository, ordenCompraRepository, service, new TipoCambioService(tipoCambioRepository));

        CuentaPorPagar cpp = cxpService.crearDesdeOrdenCompra(5L);

        assertThat(cpp.getMoneda()).isEqualTo(MonedaFE.USD);
        assertThat(cpp.getTipoCambio()).isEqualByComparingTo("500");
        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, atLeastOnce()).save(captor.capture());
        assertThat(haber(captor.getValue(), "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("56500.00");
    }

    @Test
    @DisplayName("ADR-014: el asiento de pago a proveedor conserva la fecha del pago")
    void pagoProveedor_conservaFechaDelPago() {
        PagoProveedor pago = pagoProveedor(cpp(oc("100000.00", "13000.00", "113000.00", MonedaFE.CRC, "1"),
                MonedaFE.CRC, "1"));

        AsientoContable asiento = service.generarAsientoPagoProveedor(pago);

        assertThat(asiento.getFecha()).isEqualTo(LocalDate.of(2026, 9, 25));
    }

    @Test
    @DisplayName("ADR-014: pago a proveedor sin fecha → IllegalStateException con el número del pago y el campo, sin guardar")
    void pagoProveedor_sinFecha_lanzaIllegalState() {
        PagoProveedor pago = pagoProveedor(cpp(oc("100000.00", "13000.00", "113000.00", MonedaFE.CRC, "1"),
                MonedaFE.CRC, "1"));
        pago.setFecha(null);

        assertThatThrownBy(() -> service.generarAsientoPagoProveedor(pago))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PP-2026-00001")
                .hasMessageContaining("fecha");
        verify(asientoRepository, never()).save(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PagoProveedor pagoProveedor(CuentaPorPagar cpp) {
        return PagoProveedor.builder()
                .id(20L)
                .numero("PP-2026-00001")
                .cuentaPorPagar(cpp)
                .monto(new BigDecimal("50000.00"))
                // EFECTIVO: CPP_BANCO_EFECTIVO; los medios bancarios usan la hoja de su cuenta bancaria (ADR-018)
                .metodoPago(MetodoPagoProveedor.EFECTIVO)
                .fecha(LocalDate.of(2026, 9, 25))
                .build();
    }

    private OrdenCompra oc(String subtotal, String impuesto, String total, MonedaFE moneda, String tipoCambio) {
        return OrdenCompra.builder()
                .numero("OC-1")
                .proveedor(proveedor)
                .fecha(LocalDate.of(2026, 10, 11))
                .moneda(moneda)
                .tipoCambio(tipoCambio != null ? new BigDecimal(tipoCambio) : null)
                .subtotal(new BigDecimal(subtotal))
                .impuesto(new BigDecimal(impuesto))
                .total(new BigDecimal(total))
                .build();
    }

    private CuentaPorPagar cpp(OrdenCompra oc, MonedaFE moneda, String tipoCambio) {
        return CuentaPorPagar.builder()
                .id(10L)
                .numero("CPP-2026-00001")
                .proveedor(proveedor)
                .ordenCompra(oc)
                .descripcion("Compra")
                .monto(oc.getTotal())
                .saldoPendiente(oc.getTotal())
                .moneda(moneda)
                .tipoCambio(tipoCambio != null ? new BigDecimal(tipoCambio) : null)
                .fechaEmision(LocalDate.of(2026, 10, 12))
                .build();
    }

    private BigDecimal debe(AsientoContable asiento, String clave) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuentas.get(clave))
                .map(DetalleAsiento::getDebe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal haber(AsientoContable asiento, String clave) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuentas.get(clave))
                .map(DetalleAsiento::getHaber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
