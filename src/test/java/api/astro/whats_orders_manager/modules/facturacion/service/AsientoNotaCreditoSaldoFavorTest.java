package api.astro.whats_orders_manager.modules.facturacion.service;

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
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.model.SaldoFavorCliente;
import api.astro.whats_orders_manager.modules.facturacion.util.CalculoAplicacionSaldoFavor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-048 (criterios 6, 10 y 11): el asiento de la nota con el reparto entre CxC y saldos a favor, el asiento de la
 * aplicación y el cálculo del diferencial por tramos.
 */
@DisplayName("T-048 — asientos del saldo a favor y diferencial por tramos")
class AsientoNotaCreditoSaldoFavorTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);

    private final AsientoContableRepository asientoRepository = mock(AsientoContableRepository.class);
    private final ParametroContableRepository parametros = mock(ParametroContableRepository.class);
    private final AsientoContableService servicio = new AsientoContableService(asientoRepository,
            mock(DetalleAsientoRepository.class), mock(CuentaContableRepository.class), parametros);
    private final Map<String, CuentaContable> cuentas = new HashMap<>();
    private Factura factura;

    @BeforeEach
    void setUp() {
        long id = 1;
        for (String[] c : new String[][] {{"VENTA_CXC_CLIENTES", "1.1.02.001"}, {"VENTA_IVA_POR_PAGAR", "2.1.02.001"},
                {"NOTA_CREDITO_ANULACIONES_DESCUENTOS", "4.1.02.002"}, {"SALDO_FAVOR_CLIENTES_CRC", "2.1.05.001"},
                {"SALDO_FAVOR_CLIENTES_USD", "2.1.05.002"}, {"DIFERENCIAL_CAMBIARIO_INGRESO", "4.2.01.001"},
                {"DIFERENCIAL_CAMBIARIO_GASTO", "5.2.01.001"}}) {
            conParametro(c[0], c[1], id++);
        }
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
        factura = new Factura();
        factura.setIdFactura(9);
        factura.setNumeroFactura("F-9");
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setTipoCambio(BigDecimal.ONE);
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

    private NotaFactura nota(String base, String iva) {
        BigDecimal b = new BigDecimal(base);
        BigDecimal i = new BigDecimal(iva);
        return NotaFactura.builder().id(5L).factura(factura).tipo(TipoNotaFactura.CREDITO).codigoReferencia("02")
                .estado(EstadoNotaFactura.EN_TRAMITE).subtotal(b).impuesto(i).total(b.add(i)).build();
    }

    private static BigDecimal debeDe(AsientoContable a, String codigo) {
        return a.getDetalles().stream().filter(d -> codigo.equals(d.getCuenta().getCodigo()) && d.getDebe() != null)
                .map(DetalleAsiento::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal haberDe(AsientoContable a, String codigo) {
        return a.getDetalles().stream().filter(d -> codigo.equals(d.getCuenta().getCodigo()) && d.getHaber() != null)
                .map(DetalleAsiento::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("nota con excedente (CRC): CR CxC por el crédito y CR 2.1.05.001 por el excedente; cuadra")
    void notaConExcedente() {
        AsientoContable a = servicio.generarAsientoNotaCredito(nota("1000.00", "130.00"), null, FECHA,
                new BigDecimal("630.00"), new BigDecimal("500.00"));

        assertThat(debeDe(a, "4.1.02.002")).isEqualByComparingTo("1000.00");
        assertThat(debeDe(a, "2.1.02.001")).isEqualByComparingTo("130.00");
        assertThat(haberDe(a, "1.1.02.001")).isEqualByComparingTo("630.00");
        assertThat(haberDe(a, "2.1.05.001")).isEqualByComparingTo("500.00");
        assertThat(a.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("nota íntegramente a saldo a favor: no hay línea de CxC")
    void notaSoloSaldoFavor() {
        AsientoContable a = servicio.generarAsientoNotaCredito(nota("1000.00", "130.00"), null, FECHA,
                BigDecimal.ZERO, new BigDecimal("1130.00"));

        assertThat(haberDe(a, "1.1.02.001")).isZero();
        assertThat(haberDe(a, "2.1.05.001")).isEqualByComparingTo("1130.00");
        assertThat(a.getDetalles()).noneMatch(d -> d.getHaber() != null && d.getHaber().signum() == 0
                && d.getDebe() == null);
        assertThat(a.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("nota USD con excedente: ambos créditos con el TC de la factura y el excedente absorbe el redondeo")
    void notaUsd() {
        factura.setMonedaFE(MonedaFE.USD);
        factura.setTipoCambio(new BigDecimal("512.37000"));

        AsientoContable a = servicio.generarAsientoNotaCredito(nota("33.33", "4.33"), null, FECHA,
                new BigDecimal("10.01"), new BigDecimal("27.65"));

        assertThat(haberDe(a, "1.1.02.001")).isEqualByComparingTo("5128.82");   // 10.01 × 512.37
        assertThat(haberDe(a, "2.1.05.002")).isEqualByComparingTo("14167.03");  // 37.66 × 512.37 = 19295.85, menos 5128.82
        assertThat(a.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("sin excedente no se exige el parámetro de saldos a favor; con excedente y sin parámetro → «Parámetro contable no configurado»")
    void parametroDeSaldoFavor() {
        when(parametros.findByClave("SALDO_FAVOR_CLIENTES_CRC")).thenReturn(Optional.empty());

        AsientoContable sinExcedente = servicio.generarAsientoNotaCredito(nota("1000.00", "130.00"), null, FECHA);
        assertThat(sinExcedente.estaCuadrado()).isTrue();

        assertThatThrownBy(() -> servicio.generarAsientoNotaCredito(nota("1000.00", "130.00"), null, FECHA,
                new BigDecimal("630.00"), new BigDecimal("500.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Parámetro contable no configurado: SALDO_FAVOR_CLIENTES_CRC");
    }

    @Test
    @DisplayName("aplicación en CRC: DR 2.1.05.001 / CR CxC por el monto, sin diferencial; enlazada al pago y de tipo AUTOMATICO_PAGO")
    void aplicacionCrc() {
        SaldoFavorCliente saldo = saldo("CRC", "1");
        Pago pago = new Pago();
        pago.setIdPago(70L);
        CalculoAplicacionSaldoFavor c = CalculoAplicacionSaldoFavor.calcular(new BigDecimal("300.00"), BigDecimal.ONE,
                BigDecimal.ONE, null);

        AsientoContable a = servicio.generarAsientoAplicacionSaldoFavor(saldo, factura, pago, c, FECHA);

        assertThat(debeDe(a, "2.1.05.001")).isEqualByComparingTo("300.00");
        assertThat(haberDe(a, "1.1.02.001")).isEqualByComparingTo("300.00");
        assertThat(a.getDetalles()).hasSize(2);
        assertThat(a.getPago()).isSameAs(pago);
        assertThat(a.getTipo().name()).isEqualTo("AUTOMATICO_PAGO");
        assertThat(a.getFecha()).isEqualTo(FECHA);
        assertThat(a.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("aplicación USD con pérdida y con ganancia: el diferencial va a GASTO (debe) o INGRESO (haber) y cuadra")
    void aplicacionUsd() {
        factura.setMonedaFE(MonedaFE.USD);
        SaldoFavorCliente saldo = saldo("USD", "500");
        Pago pago = new Pago();
        pago.setIdPago(70L);

        AsientoContable perdida = servicio.generarAsientoAplicacionSaldoFavor(saldo, factura, pago,
                CalculoAplicacionSaldoFavor.calcular(new BigDecimal("200.00"), new BigDecimal("500"),
                        new BigDecimal("520"), new BigDecimal("540")), FECHA);
        assertThat(debeDe(perdida, "2.1.05.002")).isEqualByComparingTo("100000.00");
        assertThat(haberDe(perdida, "1.1.02.001")).isEqualByComparingTo("104000.00");
        assertThat(debeDe(perdida, "5.2.01.001")).isEqualByComparingTo("4000.00");
        assertThat(perdida.estaCuadrado()).isTrue();

        AsientoContable ganancia = servicio.generarAsientoAplicacionSaldoFavor(saldo, factura, pago,
                CalculoAplicacionSaldoFavor.calcular(new BigDecimal("200.00"), new BigDecimal("500"),
                        new BigDecimal("480"), new BigDecimal("540")), FECHA);
        assertThat(haberDe(ganancia, "4.2.01.001")).isEqualByComparingTo("4000.00");
        assertThat(ganancia.estaCuadrado()).isTrue();
    }

    @Test
    @DisplayName("aplicación sin el parámetro de la moneda → «Parámetro contable no configurado: SALDO_FAVOR_CLIENTES_USD»")
    void aplicacionSinParametro() {
        when(parametros.findByClave("SALDO_FAVOR_CLIENTES_USD")).thenReturn(Optional.empty());
        Pago pago = new Pago();
        pago.setIdPago(70L);

        assertThatThrownBy(() -> servicio.generarAsientoAplicacionSaldoFavor(saldo("USD", "500"), factura, pago,
                CalculoAplicacionSaldoFavor.calcular(new BigDecimal("1.00"), new BigDecimal("500"),
                        new BigDecimal("500"), new BigDecimal("500")), FECHA))
                .hasMessage("Parámetro contable no configurado: SALDO_FAVOR_CLIENTES_USD");
    }

    @Test
    @DisplayName("cálculo de tramos: saldo = monto×(día−origen), CxC = monto×(día−factura), neto = CxC − saldo = débito − crédito")
    void calculoDeTramos() {
        CalculoAplicacionSaldoFavor perdida = CalculoAplicacionSaldoFavor.calcular(new BigDecimal("200.00"),
                new BigDecimal("500"), new BigDecimal("520"), new BigDecimal("540"));
        assertThat(perdida.diferencialSaldo()).isEqualByComparingTo("8000.00");
        assertThat(perdida.diferencialCxc()).isEqualByComparingTo("4000.00");
        assertThat(perdida.diferencialNeto()).isEqualByComparingTo("-4000.00");

        CalculoAplicacionSaldoFavor ganancia = CalculoAplicacionSaldoFavor.calcular(new BigDecimal("200.00"),
                new BigDecimal("500"), new BigDecimal("480"), new BigDecimal("540"));
        assertThat(ganancia.diferencialSaldo()).isEqualByComparingTo("8000.00");
        assertThat(ganancia.diferencialCxc()).isEqualByComparingTo("12000.00");
        assertThat(ganancia.diferencialNeto()).isEqualByComparingTo("4000.00");

        // con céntimos: el neto sigue siendo la diferencia de los importes redondeados del asiento
        CalculoAplicacionSaldoFavor redondeo = CalculoAplicacionSaldoFavor.calcular(new BigDecimal("33.33"),
                new BigDecimal("512.37"), new BigDecimal("498.11"), new BigDecimal("530.55"));
        assertThat(redondeo.diferencialNeto()).isEqualByComparingTo(redondeo.debeSaldoCrc().subtract(redondeo.haberCxcCrc()));
        assertThat(redondeo.diferencialNeto())
                .isEqualByComparingTo(redondeo.diferencialCxc().subtract(redondeo.diferencialSaldo()));

        CalculoAplicacionSaldoFavor crc = CalculoAplicacionSaldoFavor.calcular(new BigDecimal("300.00"), BigDecimal.ONE,
                BigDecimal.ONE, null);
        assertThat(crc.diferencialNeto()).isZero();
        assertThat(crc.diferencialSaldo()).isZero();
    }

    @Test
    @DisplayName("el saldo vivo nunca es negativo: consumir más de lo que queda se rechaza; agotar → AGOTADO; restituir → VIGENTE")
    void saldoVivo() {
        SaldoFavorCliente saldo = saldo("CRC", "1");
        saldo.setMontoOriginal(new BigDecimal("500.00"));
        saldo.setSaldoVivo(new BigDecimal("500.00"));

        assertThatThrownBy(() -> saldo.consumir(new BigDecimal("500.01"))).isInstanceOf(IllegalStateException.class);
        saldo.consumir(new BigDecimal("500.00"));
        assertThat(saldo.getEstado().name()).isEqualTo("AGOTADO");
        saldo.restituir(new BigDecimal("200.00"));
        assertThat(saldo.getEstado().name()).isEqualTo("VIGENTE");
        assertThat(saldo.getSaldoVivo()).isEqualByComparingTo("200.00");
        assertThatThrownBy(() -> saldo.restituir(new BigDecimal("300.01"))).isInstanceOf(IllegalStateException.class);
    }

    private SaldoFavorCliente saldo(String moneda, String tc) {
        SaldoFavorCliente s = new SaldoFavorCliente();
        s.setId(3L);
        s.setMoneda(moneda);
        s.setTipoCambioOrigen(new BigDecimal(tc));
        s.setNota(NotaFactura.builder().id(5L).build());
        s.setMontoOriginal(new BigDecimal("500.00"));
        s.setSaldoVivo(new BigDecimal("500.00"));
        return s;
    }
}
