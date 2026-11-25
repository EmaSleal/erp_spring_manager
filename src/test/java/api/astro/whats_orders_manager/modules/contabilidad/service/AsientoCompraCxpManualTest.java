package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorValidator;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagarLinea;
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

import static org.mockito.Mockito.mock;
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

    private static final java.time.Clock RELOJ_PRUEBA = java.time.Clock.system(java.time.ZoneId.of("America/Costa_Rica"));

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;
    @Mock private CuentaPorPagarRepository cuentaPorPagarRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private TipoCambioRepository tipoCambioRepository;

    @Mock private api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService tope;

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
                ordenCompraRepository, service, new TipoCambioService(tipoCambioRepository),
                new ComprobanteProveedorValidator(RELOJ_PRUEBA), mock(api.astro.whats_orders_manager.modules.contabilidad.repository.ProrrataIvaRepository.class),
                mock(PagoProveedorRepository.class),
                mock(UsuarioActividadService.class), RELOJ_PRUEBA, tope);
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
        when(cuentaPorPagarRepository.saveAndFlush(any(CuentaPorPagar.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaPorPagarRepository.findUltimoConsecutivo(anyInt())).thenReturn(null);

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Prueba");
        proveedor.setSaldoPendiente(BigDecimal.ZERO);
        proveedor.setDiasCredito(0);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(proveedor));
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
    @DisplayName("CxP manual con dos cuentas e IVA limitado: un débito por cuenta (base + no acreditable), DR IVA por el acreditable, CR CxP por el total")
    void manualVariasCuentas_ivaNoAcreditableSeSumaALaCuenta() {
        CuentaContable mercaderia = cuenta(51L, "5.3.01.001", TipoCuenta.EGRESO);
        CuentaPorPagar cpp = manualConLineas(MonedaFE.CRC, "1",
                linea(1, gastoServicios, "100000.00", "13000.00", "13000.00", "0.00"),
                linea(2, mercaderia, "50000.00", "6500.00", "500.00", "6000.00"));

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("100000.00");
        assertThat(debe(asiento, mercaderia)).as("50 000 de base + 6 000 no acreditable").isEqualByComparingTo("56000.00");
        assertThat(debe(asiento, cuentas.get("CPP_IVA_CREDITO_FISCAL"))).isEqualByComparingTo("13500.00");
        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("169500.00");
        assertThat(asiento.getDetalles()).hasSize(4);
        assertThat(asiento.estaCuadrado()).isTrue();
        assertThat(asiento.getFecha()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    @DisplayName("varias líneas de la misma cuenta se agrupan en un solo débito")
    void manualMismaCuenta_seAgrupa() {
        CuentaPorPagar cpp = manualConLineas(MonedaFE.CRC, "1",
                linea(1, gastoServicios, "1000.00", "130.00", "130.00", "0.00"),
                linea(2, gastoServicios, "2000.00", "260.00", "0.00", "260.00"));

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(asiento.getDetalles().stream().filter(d -> d.getCuenta() == gastoServicios)).hasSize(1);
        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("3260.00");
        assertThat(debe(asiento, cuentas.get("CPP_IVA_CREDITO_FISCAL"))).isEqualByComparingTo("130.00");
        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("3390.00");
        assertThat(asiento.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("todo el IVA no acreditable: sin débito a IVA crédito fiscal")
    void manualSinIvaAcreditable_sinLineaDeIva() {
        CuentaPorPagar cpp = manualConLineas(MonedaFE.CRC, "1",
                linea(1, gastoServicios, "1000.00", "130.00", "0.00", "130.00"));

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(asiento.getDetalles()).hasSize(2);
        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("1130.00");
        assertThat(asiento.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("USD con varias cuentas: cada cuenta se convierte a CRC y la diferencia de redondeo va a la cuenta de mayor débito; cuadra")
    void manualUsdVariasCuentas_redondeoCuadra() {
        CuentaContable otra = cuenta(52L, "5.3.01.002", TipoCuenta.EGRESO);
        // 0,05 × 503,27 = 25,16 en cada cuenta (50,32) pero el total 0,10 × 503,27 = 50,33
        CuentaPorPagar cpp = manualConLineas(MonedaFE.USD, "503.27",
                linea(1, gastoServicios, "0.05", "0.00", "0.00", "0.00"),
                linea(2, otra, "0.05", "0.00", "0.00", "0.00"));

        AsientoContable asiento = service.generarAsientoCompra(cpp);

        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("50.33");
        assertThat(debe(asiento, gastoServicios).add(debe(asiento, otra))).isEqualByComparingTo("50.33");
        assertThat(asiento.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("CxP manual sin líneas → IllegalStateException con el número, sin guardar")
    void manualSinLineas_lanzaIllegalState() {
        CuentaPorPagar sinLineas = manual("100.00", "13.00", "113.00", MonedaFE.CRC, "1");
        sinLineas.getLineas().clear();

        assertThatThrownBy(() -> service.generarAsientoCompra(sinLineas))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CPP-2026-00001");
        verify(asientoRepository, never()).save(any());
    }

    // ── crear() con ordenCompraId (T-053: factura del proveedor de la OC, por líneas) ──────────────

    @Test
    @DisplayName("crear() con una OC que ya tiene CxP admite otra factura (varias CxP por OC) y las guarda ligadas a la OC")
    void crearConOc_yaTieneCxp_admiteOtra() {
        OrdenCompra oc = oc();
        prepararOc(oc);
        CuentaPorPagar existente = CuentaPorPagar.builder().id(99L).ordenCompra(oc).monto(new BigDecimal("50.00")).build();
        when(cuentaPorPagarRepository.findByOrdenCompraAndEstadoNotForUpdate(oc,
                api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar.ANULADA))
                .thenReturn(List.of(existente));

        var creada = cxpService.crearConAdvertencias(dtoConOc(null));

        assertThat(creada.cuenta().getOrdenCompra()).isSameAs(oc);
        assertThat(creada.advertencias()).as("50 + 113 = 163 supera 113 USD por 50").hasSize(1)
                .first().asString().contains("OC-1").contains("163.00").contains("113.00").contains("por 50.00");
    }

    @Test
    @DisplayName("T-056: crear() evalúa el tope del RTS del año del comprobante; si la evaluación falla, la CxP se crea igual")
    void crearEvaluaElTopeYUnFalloNoLaBloquea() {
        prepararOc(oc());
        CuentaPorPagar cpp = cxpService.crear(dtoConOc(null));
        verify(tope).evaluarTrasRegistro(2026);
        assertThat(cpp.getMonto()).isEqualByComparingTo("113.00");

        org.mockito.Mockito.doThrow(new IllegalStateException("tope roto")).when(tope).evaluarTrasRegistro(anyInt());
        CuentaPorPagar otra = cxpService.crear(dtoConOc(null));
        assertThat(otra.getMonto()).isEqualByComparingTo("113.00");
    }

    @Test
    @DisplayName("crear() con ordenCompraId ignora el monto del DTO: el total sale de las líneas de la factura, no del total de la OC")
    void crearConOc_montoSaleDeLasLineas() {
        prepararOc(oc());

        CuentaPorPagar cpp = cxpService.crear(dtoConOc(new BigDecimal("999.00")));

        assertThat(cpp.getMonto()).isEqualByComparingTo("113.00");
        assertThat(cpp.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(cpp.getImpuesto()).isEqualByComparingTo("13.00");
        assertThat(cpp.getLineas()).hasSize(1);
        assertThat(cpp.getFechaEmision()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    @DisplayName("crear() con ordenCompraId genera el asiento por líneas (cuenta de la línea, IVA, CxP) y no usa CPP_CUENTA_COMPRAS")
    void crearConOc_asientoPorLineas() {
        OrdenCompra oc = oc();
        oc.setMoneda(MonedaFE.CRC);
        oc.setTipoCambio(BigDecimal.ONE);
        prepararOc(oc);

        cxpService.crear(dtoConOc(null));
        AsientoContable asiento = ultimoAsientoGuardado();

        assertThat(asiento.getFecha()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(debe(asiento, gastoServicios)).isEqualByComparingTo("100.00");
        assertThat(debe(asiento, cuentas.get("CPP_IVA_CREDITO_FISCAL"))).isEqualByComparingTo("13.00");
        assertThat(haber(asiento, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("113.00");
        assertThat(asiento.getDetalles()).noneMatch(d -> d.getCuenta() == cuentas.get("CPP_CUENTA_COMPRAS"));
    }

    // ── mismas reglas de moneda/TC de la OC (ADR-011) ──

    @Test
    @DisplayName("crear() con OC USD a TC 520 y dto.moneda null → CxP en USD a 520 y asiento convertido a CRC")
    void crearConOc_monedaNull_heredaMonedaYTipoCambioDeLaOc() {
        OrdenCompra oc = oc();
        oc.setTipoCambio(new BigDecimal("520.00"));
        prepararOc(oc);

        CuentaPorPagarDTO dto = dtoConOc(null);
        dto.setTipoCambio(new BigDecimal("999.00")); // con OC se ignora
        CuentaPorPagar viaCrear = cxpService.crear(dto);
        AsientoContable asientoCrear = ultimoAsientoGuardado();

        assertThat(viaCrear.getMoneda()).isEqualTo(MonedaFE.USD);
        assertThat(viaCrear.getTipoCambio()).isEqualByComparingTo("520.00");
        assertThat(haber(asientoCrear, cuentas.get("CPP_CUENTA_POR_PAGAR"))).isEqualByComparingTo("58760.00");
    }

    @Test
    @DisplayName("crear() con OC USD y dto.moneda = USD → usa la moneda y el TC de la OC")
    void crearConOc_monedaIgual_usaLaDeLaOc() {
        OrdenCompra oc = oc();
        oc.setTipoCambio(new BigDecimal("520.00"));
        prepararOc(oc);

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
        prepararOc(oc);

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
    @DisplayName("crear() con una OC no recibida → 409 con mensaje fijo, sin guardar")
    void crearConOc_noRecibida_rechaza(EstadoOrdenCompra estado) {
        OrdenCompra oc = oc();
        oc.setEstado(estado);
        prepararOc(oc);

        assertThatThrownBy(() -> cxpService.crear(dtoConOc(null)))
                .isInstanceOf(api.astro.whats_orders_manager.modules.contabilidad.service.OrdenCompraNoAdmiteFacturaException.class)
                .hasMessage("La orden de compra no admite facturas en estado " + estado
                        + "; debe estar recibida total o parcialmente");
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

    private CuentaPorPagarLinea linea(int numero, CuentaContable cuenta, String base, String iva, String acreditable,
                                      String noAcreditable) {
        return CuentaPorPagarLinea.builder().numeroLinea(numero).descripcion("Línea " + numero).cuentaContable(cuenta)
                .codigoTarifa("08").base(new BigDecimal(base)).iva(new BigDecimal(iva))
                .tratamiento(TratamientoIvaCompra.PLENO).ivaAcreditable(new BigDecimal(acreditable))
                .ivaNoAcreditable(new BigDecimal(noAcreditable)).build();
    }

    private CuentaPorPagar manualConLineas(MonedaFE moneda, String tc, CuentaPorPagarLinea... lineas) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        for (CuentaPorPagarLinea l : lineas) {
            subtotal = subtotal.add(l.getBase());
            impuesto = impuesto.add(l.getIva());
        }
        CuentaPorPagar cpp = CuentaPorPagar.builder().id(10L).numero("CPP-2026-00001").proveedor(proveedor)
                .descripcion("Servicios de mantenimiento").subtotal(subtotal).impuesto(impuesto)
                .monto(subtotal.add(impuesto)).saldoPendiente(subtotal.add(impuesto)).moneda(moneda)
                .tipoCambio(new BigDecimal(tc)).fechaEmision(LocalDate.of(2026, 9, 15)).build();
        for (CuentaPorPagarLinea l : lineas) {
            l.setCuentaPorPagar(cpp);
            cpp.getLineas().add(l);
        }
        return cpp;
    }

    private CuentaPorPagar manual(String subtotal, String impuesto, String total, MonedaFE moneda, String tc) {
        CuentaPorPagar cpp = CuentaPorPagar.builder()
                .id(10L)
                .numero("CPP-2026-00001")
                .proveedor(proveedor)
                .descripcion("Servicios de mantenimiento")
                .subtotal(new BigDecimal(subtotal))
                .impuesto(new BigDecimal(impuesto))
                .monto(new BigDecimal(total))
                .saldoPendiente(new BigDecimal(total))
                .moneda(moneda)
                .tipoCambio(new BigDecimal(tc))
                .fechaEmision(LocalDate.of(2026, 9, 15))
                .build();
        CuentaPorPagarLinea linea = linea(1, gastoServicios, subtotal, impuesto, impuesto, "0.00");
        linea.setCuentaPorPagar(cpp);
        cpp.getLineas().add(linea);
        return cpp;
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

    private void prepararOc(OrdenCompra oc) {
        when(ordenCompraRepository.findProveedorId(5L)).thenReturn(Optional.of(1L));
        when(ordenCompraRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(oc));
    }

    /** Factura del proveedor de la OC: comprobante y una línea de 100,00 + IVA 13,00 a la cuenta de gasto. */
    private CuentaPorPagarDTO dtoConOc(BigDecimal monto) {
        return CuentaPorPagarDTO.builder()
                .proveedorId(1L)
                .ordenCompraId(5L)
                .descripcion("Compra según OC OC-1")
                .monto(monto)
                .numeroComprobanteProveedor("FE-0001")
                .fechaComprobante(LocalDate.of(2026, 9, 15))
                .lineas(List.of(new api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO(
                        "Mercadería", 50L, "08", new BigDecimal("100.00"), new BigDecimal("13.00"),
                        TratamientoIvaCompra.PLENO, null)))
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
