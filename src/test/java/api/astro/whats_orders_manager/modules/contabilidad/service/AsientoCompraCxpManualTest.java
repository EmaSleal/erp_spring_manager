package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
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
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * T-012 (ADR-015, ADR-016) — asiento de compra de una CxP manual (desglose y contrapartida de la
 * propia CxP) y alta de CxP con orden de compra por {@code crear()}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AsientoContableService.generarAsientoCompra — CxP manual (ADR-016)")
class AsientoCompraCxpManualTest {

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;
    @Mock private CuentaPorPagarRepository cuentaPorPagarRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private TipoCambioRepository tipoCambioRepository;

    private AsientoContableService service;
    private CuentaPorPagarServiceImpl cxpService;
    private final Map<String, CuentaContable> cuentas = new HashMap<>();
    private CuentaContable gastoServicios;
    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);
        cxpService = new CuentaPorPagarServiceImpl(cuentaPorPagarRepository, proveedorRepository,
                ordenCompraRepository, service, new TipoCambioService(tipoCambioRepository));
        long id = 1;
        List<ParametroContable> parametros = new ArrayList<>();
        for (String clave : List.of("CPP_CUENTA_COMPRAS", "CPP_IVA_CREDITO_FISCAL", "CPP_CUENTA_POR_PAGAR")) {
            CuentaContable c = cuenta(id++, clave, TipoCuenta.EGRESO);
            cuentas.put(clave, c);
            ParametroContable p = new ParametroContable();
            p.setClave(clave);
            p.setCuentaContable(c);
            parametros.add(p);
            when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(p));
        }
        when(parametroContableRepository.findAllWithCuentas()).thenReturn(parametros);
        gastoServicios = cuenta(50L, "5.1.03.001", TipoCuenta.EGRESO);
        when(cuentaRepository.findById(50L)).thenReturn(Optional.of(gastoServicios));
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaPorPagarRepository.save(any(CuentaPorPagar.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaPorPagarRepository.findUltimoConsecutivo(anyInt())).thenReturn(null);

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Prueba");
        proveedor.setSaldoPendiente(BigDecimal.ZERO);
        proveedor.setDiasCredito(0);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
    }

    @Test
    @DisplayName("CxP manual CRC 100 000 + IVA 13 000 → DR contrapartida 100 000 / DR IVA 13 000 / CR CxP 113 000, fechado en la emisión")
    void manualCrc_desgloseYContrapartida() {
        CuentaPorPagar cpp = manual("100000.00", "13000.00", "113000.00", MonedaFE.CRC, "1");

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_COMPRA);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(asiento.getCuentaPorPagar()).isSameAs(cpp);
        assertThat(asiento.getFecha()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("100000.00");
        assertThat(debe(asiento, cuentas.get("CPP_IVA_CREDITO_FISCAL"))).isEqualByComparingTo("13000.00");
        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("113000.00");
        assertThat(asiento.getDetalles()).hasSize(3)
                .noneMatch(d -> d.getCuenta() == cuentas.get("CPP_CUENTA_COMPRAS"));
        assertThat(asiento.getConcepto())
                .isEqualTo("CxP manual CPP-2026-00001 - Proveedor Prueba - Servicios de mantenimiento");
    }

    @Test
    @DisplayName("CxP manual exenta (impuesto 0): sin línea de IVA y cuadra")
    void manualExenta_sinLineaIva() {
        CuentaPorPagar cpp = manual("50000.00", "0.00", "50000.00", MonedaFE.CRC, "1");

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(asiento.getDetalles()).hasSize(2)
                .noneMatch(d -> d.getCuenta() == cuentas.get("CPP_IVA_CREDITO_FISCAL"));
        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("50000.00");
        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("50000.00");
        assertThat(asiento.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("CxP manual USD 100 + 13 a TC 520 → DR 52 000 / DR 6 760 / CR 58 760")
    void manualUsd_convierteACrc() {
        CuentaPorPagar cpp = manual("100.00", "13.00", "113.00", MonedaFE.USD, "520.00");

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("52000.00");
        assertThat(debe(asiento, cuentas.get("CPP_IVA_CREDITO_FISCAL"))).isEqualByComparingTo("6760.00");
        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("58760.00");
        assertThat(asiento.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("CxP manual USD con redondeo (TC 503.27): el centavo va a la línea de gasto y cuadra")
    void manualUsd_redondeoEnLineaDeGasto() {
        // 10.05 × 503.27 = 5057.86 ; 1.31 × 503.27 = 659.28 ; 11.36 × 503.27 = 5717.15 → gasto 5057.87
        CuentaPorPagar cpp = manual("10.05", "1.31", "11.36", MonedaFE.USD, "503.27");

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("5717.15");
        assertThat(debe(asiento, cuentas.get("CPP_IVA_CREDITO_FISCAL"))).isEqualByComparingTo("659.28");
        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("5057.87");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        assertThat(asiento.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("CxP manual sin subtotal, impuesto o cuenta de gasto → IllegalStateException con el número, sin guardar")
    void manualIncompleta_lanzaIllegalState() {
        CuentaPorPagar sinSubtotal = manual("100.00", "13.00", "113.00", MonedaFE.CRC, "1");
        sinSubtotal.setSubtotal(null);
        CuentaPorPagar sinImpuesto = manual("100.00", "13.00", "113.00", MonedaFE.CRC, "1");
        sinImpuesto.setImpuesto(null);
        CuentaPorPagar sinCuenta = manual("100.00", "13.00", "113.00", MonedaFE.CRC, "1");
        sinCuenta.setCuentaGasto(null);

        for (CuentaPorPagar cpp : List.of(sinSubtotal, sinImpuesto, sinCuenta)) {
            assertThatThrownBy(() -> service.generarAsientoCompra(cpp))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CPP-2026-00001");
        }
        verify(asientoRepository, never()).save(any());
    }

    // ── crear() con ordenCompraId ────────────────────────────────────────────

    @Test
    @DisplayName("crear() con una OC que ya tiene CxP → rechazo sin guardar")
    void crearConOc_yaTieneCxp_rechaza() {
        OrdenCompra oc = oc();
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc));
        when(cuentaPorPagarRepository.existsByOrdenCompra(oc)).thenReturn(true);

        assertThatThrownBy(() -> cxpService.crear(dtoConOc(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OC-1");
        verify(cuentaPorPagarRepository, never()).save(any());
        verify(asientoRepository, never()).save(any());
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() con ordenCompraId y monto distinto de oc.total → rechazo sin guardar")
    void crearConOc_montoDistinto_rechaza() {
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc()));

        assertThatThrownBy(() -> cxpService.crear(dtoConOc(new BigDecimal("112.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("113.00");
        verify(cuentaPorPagarRepository, never()).save(any());
        verify(asientoRepository, never()).save(any());
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() con ordenCompraId válido genera el mismo asiento que crearDesdeOrdenCompra e ignora el desglose manual")
    void crearConOc_mismoAsientoQueCrearDesdeOrdenCompra() {
        OrdenCompra oc = oc();
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc));

        CuentaPorPagarDTO dto = dtoConOc(new BigDecimal("113.00"));
        dto.setSubtotal(new BigDecimal("1.00"));
        dto.setImpuesto(new BigDecimal("2.00"));
        dto.setCuentaGastoId(50L);
        CuentaPorPagar viaCrear = cxpService.crear(dto);
        AsientoContable asientoCrear = ultimoAsientoGuardado();

        CuentaPorPagar viaOc = cxpService.crearDesdeOrdenCompra(5L);
        AsientoContable asientoOc = ultimoAsientoGuardado();

        assertThat(viaCrear.getSubtotal()).isNull();
        assertThat(viaCrear.getImpuesto()).isNull();
        assertThat(viaCrear.getCuentaGasto()).isNull();
        assertThat(viaCrear.getMonto()).isEqualByComparingTo(viaOc.getMonto());
        assertThat(asientoCrear).isNotSameAs(asientoOc);
        assertThat(asientoCrear.getTipo()).isEqualTo(asientoOc.getTipo());
        assertThat(asientoCrear.getConcepto()).isEqualTo(asientoOc.getConcepto());
        assertThat(lineas(asientoCrear)).isEqualTo(lineas(asientoOc));
        assertThat(debe(asientoCrear, cuentas.get("CPP_CUENTA_COMPRAS"))).isEqualByComparingTo("56500.00");
        assertThat(debe(asientoCrear, cuentas.get("CPP_IVA_CREDITO_FISCAL"))).isEqualByComparingTo("7345.00");
        assertThat(haber(asientoCrear, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("63845.00");
    }

    // ── Revisión 1: mismas reglas de moneda/TC y estado que crearDesdeOrdenCompra ──

    @Test
    @DisplayName("crear() con OC USD a TC 520 y dto.moneda null → CxP en USD a 520 y mismo asiento que crearDesdeOrdenCompra")
    void crearConOc_monedaNull_heredaMonedaYTipoCambioDeLaOc() {
        OrdenCompra oc = oc();
        oc.setTipoCambio(new BigDecimal("520.00"));
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc));

        CuentaPorPagarDTO dto = dtoConOc(null);
        dto.setTipoCambio(new BigDecimal("999.00")); // con OC se ignora
        CuentaPorPagar viaCrear = cxpService.crear(dto);
        AsientoContable asientoCrear = ultimoAsientoGuardado();

        CuentaPorPagar viaOc = cxpService.crearDesdeOrdenCompra(5L);
        AsientoContable asientoOc = ultimoAsientoGuardado();

        assertThat(viaCrear.getMoneda()).isEqualTo(MonedaFE.USD);
        assertThat(viaCrear.getTipoCambio()).isEqualByComparingTo("520.00");
        assertThat(viaCrear.getTipoCambio()).isEqualByComparingTo(viaOc.getTipoCambio());
        assertThat(lineas(asientoCrear)).isEqualTo(lineas(asientoOc));
        assertThat(haber(asientoCrear, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("58760.00");
    }

    @Test
    @DisplayName("crear() con OC USD y dto.moneda = USD → usa la moneda y el TC de la OC")
    void crearConOc_monedaIgual_usaLaDeLaOc() {
        OrdenCompra oc = oc();
        oc.setTipoCambio(new BigDecimal("520.00"));
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc));

        CuentaPorPagarDTO dto = dtoConOc(null);
        dto.setMoneda(MonedaFE.USD);
        CuentaPorPagar cpp = cxpService.crear(dto);

        assertThat(cpp.getMoneda()).isEqualTo(MonedaFE.USD);
        assertThat(cpp.getTipoCambio()).isEqualByComparingTo("520.00");
    }

    @Test
    @DisplayName("crear() con OC USD y dto.moneda = CRC → IllegalArgumentException con ambas monedas, sin guardar")
    void crearConOc_monedaDistinta_rechaza() {
        OrdenCompra oc = oc();
        oc.setTipoCambio(new BigDecimal("520.00"));
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc));

        CuentaPorPagarDTO dto = dtoConOc(null);
        dto.setMoneda(MonedaFE.CRC);

        assertThatThrownBy(() -> cxpService.crear(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CRC")
                .hasMessageContaining("USD");
        verify(cuentaPorPagarRepository, never()).save(any());
        verify(asientoRepository, never()).save(any());
        verify(proveedorRepository, never()).save(any());
    }

    @ParameterizedTest(name = "OC en {0}")
    @EnumSource(value = EstadoOrdenCompra.class, names = {"RECIBIDA", "RECIBIDA_PARCIAL"},
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("crear() con una OC no recibida → mismo rechazo que crearDesdeOrdenCompra, sin guardar")
    void crearConOc_noRecibida_rechaza(EstadoOrdenCompra estado) {
        OrdenCompra oc = oc();
        oc.setEstado(estado);
        when(ordenCompraRepository.findById(5L)).thenReturn(Optional.of(oc));

        assertThatThrownBy(() -> cxpService.crear(dtoConOc(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Solo se puede generar CPP de una OC recibida");
        assertThatThrownBy(() -> cxpService.crearDesdeOrdenCompra(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Solo se puede generar CPP de una OC recibida");
        verify(cuentaPorPagarRepository, never()).save(any());
        verify(asientoRepository, never()).save(any());
        verify(proveedorRepository, never()).save(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private CuentaContable cuenta(long id, String codigo, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre(codigo);
        c.setTipo(tipo);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return c;
    }

    private CuentaPorPagar manual(String subtotal, String impuesto, String total, MonedaFE moneda, String tc) {
        return CuentaPorPagar.builder()
                .id(10L)
                .numero("CPP-2026-00001")
                .proveedor(proveedor)
                .descripcion("Servicios de mantenimiento")
                .subtotal(new BigDecimal(subtotal))
                .impuesto(new BigDecimal(impuesto))
                .monto(new BigDecimal(total))
                .saldoPendiente(new BigDecimal(total))
                .cuentaGasto(gastoServicios)
                .moneda(moneda)
                .tipoCambio(new BigDecimal(tc))
                .fechaEmision(LocalDate.of(2026, 9, 15))
                .build();
    }

    private OrdenCompra oc() {
        return OrdenCompra.builder()
                .id(5L)
                .numero("OC-1")
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.RECIBIDA)
                .fecha(LocalDate.of(2026, 10, 11))
                .moneda(MonedaFE.USD)
                .tipoCambio(new BigDecimal("565"))
                .subtotal(new BigDecimal("100.00"))
                .impuesto(new BigDecimal("13.00"))
                .total(new BigDecimal("113.00"))
                .build();
    }

    private CuentaPorPagarDTO dtoConOc(BigDecimal monto) {
        return CuentaPorPagarDTO.builder()
                .proveedorId(1L)
                .ordenCompraId(5L)
                .descripcion("Compra según OC OC-1")
                .monto(monto)
                .build();
    }

    private AsientoContable ultimoAsientoGuardado() {
        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    /** cuenta|debe|haber de cada línea, para comparar dos asientos. */
    private List<String> lineas(AsientoContable asiento) {
        return asiento.getDetalles().stream()
                .map(d -> d.getCuenta().getCodigo() + "|" + d.getDebe().stripTrailingZeros().toPlainString()
                        + "|" + d.getHaber().stripTrailingZeros().toPlainString())
                .toList();
    }

    private BigDecimal debe(AsientoContable asiento, CuentaContable cuenta) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuenta)
                .map(DetalleAsiento::getDebe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal haber(AsientoContable asiento, CuentaContable cuenta) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuenta)
                .map(DetalleAsiento::getHaber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
