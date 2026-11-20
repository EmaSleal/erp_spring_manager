package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-047 / ADR-052 / ADR-054 (criterios 8, 21, 24, 26, 27 y 29): asiento de la nota de crédito y el único punto de
 * decisión de la cuenta que recibe el débito por la base.
 */
@DisplayName("T-047 — asiento de la nota de crédito y mapeo código → cuenta de la base")
class AsientoNotaCreditoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 12);

    private final AsientoContableRepository asientoRepository = mock(AsientoContableRepository.class);
    private final ParametroContableRepository parametros = mock(ParametroContableRepository.class);
    private final AsientoContableService servicio = new AsientoContableService(asientoRepository,
            mock(DetalleAsientoRepository.class), mock(CuentaContableRepository.class), parametros);
    private final Map<String, CuentaContable> cuentas = new HashMap<>();
    private AsientoContable venta;
    private Factura factura;

    @BeforeEach
    void setUp() {
        long id = 1;
        for (String[] c : new String[][] {{"VENTA_CXC_CLIENTES", "1.1.02.001"}, {"VENTA_INGRESOS", "4.1.01.001"},
                {"VENTA_IVA_POR_PAGAR", "2.1.02.001"}, {"NOTA_CREDITO_DEVOLUCIONES", "4.1.02.001"},
                {"NOTA_CREDITO_ANULACIONES_DESCUENTOS", "4.1.02.002"}}) {
            conParametro(c[0], c[1], id++);
        }
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
        factura = new Factura();
        factura.setIdFactura(9);
        factura.setNumeroFactura("F-9");
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setTipoCambio(BigDecimal.ONE);
        venta = ventaOriginal();
    }

    private void conParametro(String clave, String codigo, long id) {
        CuentaContable cuenta = new CuentaContable();
        cuenta.setIdCuenta(id);
        cuenta.setCodigo(codigo);
        cuenta.setNombre(codigo);
        cuenta.setActiva(true);
        cuenta.setAceptaMovimientos(true);
        cuentas.put(clave, cuenta);
        ParametroContable p = new ParametroContable();
        p.setClave(clave);
        p.setCuentaContable(cuenta);
        when(parametros.findByClave(clave)).thenReturn(Optional.of(p));
    }

    /** Asiento de venta: DR CxC 1130, CR ventas 1000, CR IVA 130. */
    private AsientoContable ventaOriginal() {
        AsientoContable a = new AsientoContable();
        a.setNumero("ASI-2026-0001");
        List<DetalleAsiento> d = new ArrayList<>();
        d.add(detalle(cuentas.get("VENTA_CXC_CLIENTES"), "1130.00", null));
        d.add(detalle(cuentas.get("VENTA_INGRESOS"), null, "1000.00"));
        d.add(detalle(cuentas.get("VENTA_IVA_POR_PAGAR"), null, "130.00"));
        a.setDetalles(d);
        return a;
    }

    private static DetalleAsiento detalle(CuentaContable cuenta, String debe, String haber) {
        DetalleAsiento d = new DetalleAsiento();
        d.setCuenta(cuenta);
        d.setDebe(debe != null ? new BigDecimal(debe) : null);
        d.setHaber(haber != null ? new BigDecimal(haber) : null);
        return d;
    }

    private NotaFactura nota(String codigo, String base, String iva) {
        BigDecimal b = new BigDecimal(base);
        BigDecimal i = new BigDecimal(iva);
        return NotaFactura.builder().id(5L).factura(factura).tipo(TipoNotaFactura.CREDITO).codigoReferencia(codigo)
                .estado(EstadoNotaFactura.EN_TRAMITE).subtotal(b).impuesto(i).total(b.add(i)).build();
    }

    private static BigDecimal debeDe(AsientoContable a, String codigoCuenta) {
        return a.getDetalles().stream().filter(d -> codigoCuenta.equals(d.getCuenta().getCodigo()) && d.getDebe() != null)
                .map(DetalleAsiento::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal haberDe(AsientoContable a, String codigoCuenta) {
        return a.getDetalles().stream().filter(d -> codigoCuenta.equals(d.getCuenta().getCodigo()) && d.getHaber() != null)
                .map(DetalleAsiento::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"01", "02"})
    @DisplayName("01 total y 02 parcial: DR 4.1.02.002 por la base, DR IVA por pagar, CR cuentas por cobrar; cuadra")
    void anulacionesYDescuentos(String codigo) {
        AsientoContable a = servicio.generarAsientoNotaCredito(nota(codigo, "1000.00", "130.00"), venta, FECHA);

        assertThat(debeDe(a, "4.1.02.002")).isEqualByComparingTo("1000.00");
        assertThat(debeDe(a, "2.1.02.001")).isEqualByComparingTo("130.00");
        assertThat(haberDe(a, "1.1.02.001")).isEqualByComparingTo("1130.00");
        assertThat(a.estaCuadrado()).isTrue();
        assertThat(a.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        // T-047 criterio 33: tipo propio, distinto del asiento de venta
        assertThat(a.getTipo().name()).isEqualTo("AUTOMATICO_NOTA_CREDITO");
        assertThat(a.getTipo().esAutomatico()).isTrue();
        assertThat(a.getFecha()).isEqualTo(FECHA);
        assertThat(a.getNotaFactura().getId()).isEqualTo(5L);
        assertThat(a.getAsientoRevertido()).isNull();
        assertThat(a.getDetalles()).noneMatch(d -> "4.1.01.001".equals(d.getCuenta().getCodigo()));
    }

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"13", "14"})
    @DisplayName("13 total y 14 parcial: el débito por la base va a la cuenta de ventas del asiento original")
    void errorMaterial(String codigo) {
        AsientoContable a = servicio.generarAsientoNotaCredito(nota(codigo, "500.00", "65.00"), venta, FECHA);

        assertThat(debeDe(a, "4.1.01.001")).isEqualByComparingTo("500.00");
        assertThat(debeDe(a, "2.1.02.001")).isEqualByComparingTo("65.00");
        assertThat(haberDe(a, "1.1.02.001")).isEqualByComparingTo("565.00");
        assertThat(a.estaCuadrado()).isTrue();
        assertThat(a.getDetalles()).noneMatch(d -> d.getCuenta().getCodigo().startsWith("4.1.02"));
    }

    @Test
    @DisplayName("nota exenta: sin línea de IVA y cuadra")
    void sinIva() {
        AsientoContable a = servicio.generarAsientoNotaCredito(nota("02", "250.00", "0.00"), venta, FECHA);

        assertThat(a.getDetalles()).hasSize(2);
        assertThat(a.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("moneda extranjera: al tipo de cambio de la factura, el residuo de redondeo va a la base y el asiento cuadra")
    void tipoCambio() {
        factura.setMonedaFE(MonedaFE.USD);
        factura.setTipoCambio(new BigDecimal("512.34000"));

        for (String[] caso : new String[][] {{"0.01", "0.00"}, {"88.50", "11.50"}, {"33.33", "4.33"}, {"7.77", "1.01"}}) {
            AsientoContable a = servicio.generarAsientoNotaCredito(nota("02", caso[0], caso[1]), venta, FECHA);
            BigDecimal total = new BigDecimal(caso[0]).add(new BigDecimal(caso[1]));
            assertThat(haberDe(a, "1.1.02.001")).isEqualByComparingTo(
                    total.multiply(new BigDecimal("512.34")).setScale(2, java.math.RoundingMode.HALF_UP));
            assertThat(a.estaCuadrado()).as(caso[0] + "+" + caso[1]).isTrue();
        }
    }

    @Test
    @DisplayName("parámetro ausente: falla y el mensaje nombra el parámetro (la nota sigue EN_TRAMITE)")
    void parametroAusente() {
        when(parametros.findByClave("NOTA_CREDITO_ANULACIONES_DESCUENTOS")).thenReturn(Optional.empty());
        when(parametros.findByClave("NOTA_CREDITO_DEVOLUCIONES")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.generarAsientoNotaCredito(nota("01", "1000.00", "130.00"), venta, FECHA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOTA_CREDITO_ANULACIONES_DESCUENTOS");
        assertThatThrownBy(() -> servicio.resolverCuentaBaseNota("06", venta))
                .hasMessageContaining("NOTA_CREDITO_DEVOLUCIONES");
        // 13 y 14 no dependen de los parámetros nuevos
        assertThat(servicio.resolverCuentaBaseNota("13", venta).getCodigo()).isEqualTo("4.1.01.001");
    }

    @Test
    @DisplayName("punto de decisión: 01 y 02 → 4.1.02.002; 06 → 4.1.02.001; 13 y 14 → ventas")
    void mapeoPorCodigo() {
        assertThat(servicio.resolverCuentaBaseNota("01", venta).getCodigo()).isEqualTo("4.1.02.002");
        assertThat(servicio.resolverCuentaBaseNota("02", venta).getCodigo()).isEqualTo("4.1.02.002");
        assertThat(servicio.resolverCuentaBaseNota("06", venta).getCodigo()).isEqualTo("4.1.02.001");
        assertThat(servicio.resolverCuentaBaseNota("13", venta).getCodigo()).isEqualTo("4.1.01.001");
        assertThat(servicio.resolverCuentaBaseNota("14", venta).getCodigo()).isEqualTo("4.1.01.001");
    }

    @Test
    @DisplayName("código 12: rechazo claro (P-105) y nunca a 4.1.02; código no mapeado o desconocido: rechazo, sin rama por defecto")
    void codigosRechazados() {
        assertThatThrownBy(() -> servicio.resolverCuentaBaseNota("12", venta))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nota 12 por exoneración aún no soportada");
        for (String codigo : new String[] {"04", "05", "07", "08", "09", "10", "11", "15", "16", "17", "99", "03", "XX", null}) {
            assertThatThrownBy(() -> servicio.resolverCuentaBaseNota(codigo, venta))
                    .as(String.valueOf(codigo)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("13 y 14 sin asiento de venta original → rechazo claro")
    void sinVentaOriginal() {
        assertThatThrownBy(() -> servicio.resolverCuentaBaseNota("14", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("asiento de venta");
    }
}
