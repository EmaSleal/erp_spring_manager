package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-005 — Contabilización automática de ventas y cobros (ADR-008, reglas 1, 2, 3, 4 y 6).
 *
 * Pruebas unitarias de los generadores de AsientoContableService:
 * generarAsientoVenta, generarAsientoPago, generarAsientoReversion y sincronizarAsientoVenta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AsientoContableService — asientos de venta, cobro y reversión (ADR-008)")
class AsientoVentaCobroServiceTest {

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private AsientoContableService service;

    /** Cuentas por clave contable, con IDs distintos para poder agrupar por cuenta. */
    private final Map<String, CuentaContable> cuentas = new HashMap<>();

    private static final List<String> CLAVES = List.of(
            "VENTA_CXC_CLIENTES", "VENTA_INGRESOS", "VENTA_IVA_POR_PAGAR", "COBRO_ANTICIPOS_CLIENTES",
            "COBRO_CAJA_EFECTIVO", "COBRO_CAJA_TARJETA", "COBRO_CAJA_CHEQUE",
            "COBRO_CAJA_TRANSFERENCIA", "COBRO_CAJA_RECAUDADO", "COBRO_CAJA_OTROS");

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);

        long id = 1;
        for (String clave : CLAVES) {
            CuentaContable cuenta = cuenta(id++, clave);
            cuentas.put(clave, cuenta);
            ParametroContable param = new ParametroContable();
            param.setClave(clave);
            param.setCuentaContable(cuenta);
            when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(param));
        }
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================================
    // Venta
    // =========================================================================

    @Nested
    @DisplayName("generarAsientoVenta")
    class Venta {

        @Test
        @DisplayName("factura CRC 100000 + 13000 → DR CxC 113000 / CR Ingresos 100000 / CR IVA 13000, contabilizado y vinculado")
        void ventaCrc_generaAsientoCuadradoYContabilizado() {
            Factura factura = factura(42, "100000.00", "13000.00", "113000.00", MonedaFE.CRC, BigDecimal.ONE);

            AsientoContable asiento = service.generarAsientoVenta(factura);

            assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_VENTA);
            assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(asiento.getFactura()).isSameAs(factura);
            assertThat(debe(asiento, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("113000.00");
            assertThat(haber(asiento, "VENTA_INGRESOS")).isEqualByComparingTo("100000.00");
            assertThat(haber(asiento, "VENTA_IVA_POR_PAGAR")).isEqualByComparingTo("13000.00");
            assertThat(asiento.getDetalles()).hasSize(3);
        }

        @Test
        @DisplayName("criterio 9: el asiento de venta cuadra (totalDebe == totalHaber)")
        void ventaCuadra() {
            AsientoContable asiento = service.generarAsientoVenta(
                    factura(1, "100000.00", "13000.00", "113000.00", MonedaFE.CRC, BigDecimal.ONE));

            assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        }

        @Test
        @DisplayName("criterio 2: existeAsientoVentaParaFactura recibe el id real de la factura (no null)")
        void verificacionDuplicado_usaIdReal() {
            Factura factura = factura(42, "100.00", "13.00", "113.00", MonedaFE.CRC, BigDecimal.ONE);

            service.generarAsientoVenta(factura);

            ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
            verify(asientoRepository).existeAsientoVentaParaFactura(captor.capture());
            assertThat(captor.getValue()).isEqualTo(42L);
        }

        @Test
        @DisplayName("si ya existe un asiento vigente para la factura, no se genera otro")
        void duplicado_lanzaExcepcion() {
            when(asientoRepository.existeAsientoVentaParaFactura(42L)).thenReturn(true);

            assertThatThrownBy(() -> service.generarAsientoVenta(
                    factura(42, "100.00", "13.00", "113.00", MonedaFE.CRC, BigDecimal.ONE)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("criterio 1: si falta la clave VENTA_INGRESOS → IllegalStateException con el nombre de la clave")
        void claveFaltante_lanzaIllegalState() {
            when(parametroContableRepository.findByClave("VENTA_INGRESOS")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generarAsientoVenta(
                    factura(1, "100.00", "13.00", "113.00", MonedaFE.CRC, BigDecimal.ONE)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("VENTA_INGRESOS");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("criterio 4: factura USD se convierte a CRC con tipoCambio (HALF_UP) y el centavo se ajusta en ingresos")
        void ventaUsd_convierteACrcYAjustaCentavo() {
            // 11.36 * 505.555 = 5743.10 ; 1.31 * 505.555 = 662.28 ; 10.05 * 505.555 = 5080.83
            // 5080.83 + 662.28 = 5743.11 → ingresos se ajusta a 5080.82 para cuadrar
            Factura factura = factura(7, "10.05", "1.31", "11.36", MonedaFE.USD, new BigDecimal("505.555"));

            AsientoContable asiento = service.generarAsientoVenta(factura);

            assertThat(debe(asiento, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("5743.10");
            assertThat(haber(asiento, "VENTA_IVA_POR_PAGAR")).isEqualByComparingTo("662.28");
            assertThat(haber(asiento, "VENTA_INGRESOS")).isEqualByComparingTo("5080.82");
            assertThat(asiento.estaCuadrado()).isTrue();
        }

        @Test
        @DisplayName("criterio 4: factura USD sin tipo de cambio → falla con mensaje claro")
        void ventaUsdSinTipoCambio_falla() {
            Factura factura = factura(7, "100.00", "13.00", "113.00", MonedaFE.USD, null);

            assertThatThrownBy(() -> service.generarAsientoVenta(factura))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("tipo de cambio");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("criterio 9: un asiento de venta que no cuadra lanza IllegalStateException antes de guardar")
        void ventaDescuadrada_lanzaIllegalState() {
            // total no coincide con subtotal + impuesto
            Factura factura = factura(3, "100.00", "13.00", "200.00", MonedaFE.CRC, BigDecimal.ONE);

            assertThatThrownBy(() -> service.generarAsientoVenta(factura))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no cuadra");
            verify(asientoRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Cobro
    // =========================================================================

    @Nested
    @DisplayName("generarAsientoPago")
    class Cobro {

        @Test
        @DisplayName("pago con factura → DR COBRO_CAJA_<método> / CR VENTA_CXC_CLIENTES por el monto")
        void cobroConFactura() {
            Factura factura = factura(5, "100000.00", "13000.00", "113000.00", MonedaFE.CRC, BigDecimal.ONE);
            Pago pago = pago(9L, "50000.00", MetodoPago.TRANSFERENCIA, EstadoPago.CONFIRMADO, factura);

            AsientoContable asiento = service.generarAsientoPago(pago);

            assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_PAGO);
            assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(asiento.getPago()).isSameAs(pago);
            assertThat(debe(asiento, "COBRO_CAJA_TRANSFERENCIA")).isEqualByComparingTo("50000.00");
            assertThat(haber(asiento, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("50000.00");
            assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        }

        @Test
        @DisplayName("adelanto sin factura → acredita COBRO_ANTICIPOS_CLIENTES y no hay NPE")
        void adelantoSinFactura() {
            Pago pago = pago(10L, "25000.00", MetodoPago.EFECTIVO, EstadoPago.CONFIRMADO, null);

            AsientoContable asiento = service.generarAsientoPago(pago);

            assertThat(debe(asiento, "COBRO_CAJA_EFECTIVO")).isEqualByComparingTo("25000.00");
            assertThat(haber(asiento, "COBRO_ANTICIPOS_CLIENTES")).isEqualByComparingTo("25000.00");
            assertThat(haber(asiento, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("cada MetodoPago resuelve su propia clave COBRO_CAJA_<método>")
        void cadaMetodoUsaSuClave() {
            long id = 100;
            for (MetodoPago metodo : MetodoPago.values()) {
                Pago pago = pago(id++, "10.00", metodo, EstadoPago.CONFIRMADO, null);
                AsientoContable asiento = service.generarAsientoPago(pago);
                assertThat(debe(asiento, "COBRO_CAJA_" + metodo.name())).isEqualByComparingTo("10.00");
            }
        }

        @Test
        @DisplayName("criterio 4: cobro de factura USD usa factura.tipoCambio (sin diferencial)")
        void cobroUsd_usaTipoCambioDeLaFactura() {
            Factura factura = factura(6, "100.00", "13.00", "113.00", MonedaFE.USD, new BigDecimal("510.25"));
            Pago pago = pago(11L, "50.00", MetodoPago.TARJETA, EstadoPago.CONFIRMADO, factura);

            AsientoContable asiento = service.generarAsientoPago(pago);

            assertThat(debe(asiento, "COBRO_CAJA_TARJETA")).isEqualByComparingTo("25512.50");
            assertThat(haber(asiento, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("25512.50");
        }

        @Test
        @DisplayName("criterio 1: si falta COBRO_CAJA_CHEQUE → IllegalStateException con el nombre de la clave")
        void claveCajaFaltante() {
            when(parametroContableRepository.findByClave("COBRO_CAJA_CHEQUE")).thenReturn(Optional.empty());
            Pago pago = pago(12L, "10.00", MetodoPago.CHEQUE, EstadoPago.CONFIRMADO, null);

            assertThatThrownBy(() -> service.generarAsientoPago(pago))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COBRO_CAJA_CHEQUE");
        }

        @Test
        @DisplayName("PENDIENTE y RECHAZADO no se contabilizan")
        void pagoNoValido_noSeContabiliza() {
            assertThatThrownBy(() -> service.generarAsientoPago(
                    pago(13L, "10.00", MetodoPago.EFECTIVO, EstadoPago.PENDIENTE, null)))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service.generarAsientoPago(
                    pago(14L, "10.00", MetodoPago.EFECTIVO, EstadoPago.RECHAZADO, null)))
                    .isInstanceOf(IllegalStateException.class);
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("si el pago ya tiene asiento vigente no se genera otro")
        void pagoYaContabilizado() {
            when(asientoRepository.existeAsientoPagoParaPago(15L)).thenReturn(true);

            assertThatThrownBy(() -> service.generarAsientoPago(
                    pago(15L, "10.00", MetodoPago.EFECTIVO, EstadoPago.CONCILIADO, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(asientoRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Reversión
    // =========================================================================

    @Nested
    @DisplayName("generarAsientoReversion")
    class Reversion {

        @Test
        @DisplayName("líneas espejo, contabilizado, vinculado al original y suma por cuenta cero")
        void reversionEspejo() {
            Pago pago = pago(20L, "30000.00", MetodoPago.EFECTIVO, EstadoPago.CONFIRMADO, null);
            AsientoContable original = service.generarAsientoPago(pago);
            original.setIdAsiento(500L);
            original.setNumero("ASI-2026-0500");

            AsientoContable reversion = service.generarAsientoReversion(original, "Pago anulado");

            assertThat(reversion.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(reversion.getTipo()).isEqualTo(original.getTipo());
            assertThat(reversion.getAsientoRevertido()).isSameAs(original);
            assertThat(reversion.getPago()).isSameAs(pago);
            assertThat(reversion.getConcepto()).contains("ASI-2026-0500");
            assertThat(reversion.getTotalDebe()).isEqualByComparingTo(reversion.getTotalHaber());
            assertThat(saldoPorCuenta(original, reversion)).allSatisfy(
                    (cuenta, saldo) -> assertThat(saldo).isEqualByComparingTo("0"));
            // El original no se modifica
            assertThat(original.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(debe(original, "COBRO_CAJA_EFECTIVO")).isEqualByComparingTo("30000.00");
        }

        @Test
        @DisplayName("no se revierte un asiento que no está contabilizado ni uno ya revertido")
        void reversionInvalida() {
            AsientoContable borrador = new AsientoContable();
            borrador.setEstado(EstadoAsiento.BORRADOR);
            assertThatThrownBy(() -> service.generarAsientoReversion(borrador, "x"))
                    .isInstanceOf(IllegalStateException.class);

            AsientoContable original = service.generarAsientoPago(
                    pago(21L, "10.00", MetodoPago.EFECTIVO, EstadoPago.CONFIRMADO, null));
            original.setIdAsiento(501L);
            when(asientoRepository.existsByAsientoRevertidoIdAsiento(501L)).thenReturn(true);
            assertThatThrownBy(() -> service.generarAsientoReversion(original, "x"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya fue revertido");
        }

        @Test
        @DisplayName("criterio 9: una reversión descuadrada lanza IllegalStateException antes de guardar")
        void reversionDescuadrada() {
            AsientoContable original = new AsientoContable();
            original.setEstado(EstadoAsiento.CONTABILIZADO);
            original.setNumero("ASI-2026-0999");
            original.setTipo(TipoAsiento.AUTOMATICO_PAGO);
            DetalleAsiento linea = new DetalleAsiento();
            linea.setCuenta(cuentas.get("COBRO_CAJA_EFECTIVO"));
            linea.setDebe(new BigDecimal("10.00"));
            original.getDetalles().add(linea);

            assertThatThrownBy(() -> service.generarAsientoReversion(original, "x"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no cuadra");
            verify(asientoRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Sincronización tras edición de factura
    // =========================================================================

    @Nested
    @DisplayName("sincronizarAsientoVenta")
    class Sincronizacion {

        @Test
        @DisplayName("sin cambios de montos no genera reversión ni asiento nuevo")
        void sinCambios() {
            Factura factura = factura(30, "100.00", "13.00", "113.00", MonedaFE.CRC, BigDecimal.ONE);
            AsientoContable vigente = service.generarAsientoVenta(factura);
            org.mockito.Mockito.clearInvocations(asientoRepository);
            when(asientoRepository.findAsientosVentaVigentes(30L)).thenReturn(List.of(vigente));

            assertThat(service.sincronizarAsientoVenta(factura)).isNull();
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("factura sin asiento (histórica) no se contabiliza")
        void sinAsientoVigente() {
            Factura factura = factura(31, "100.00", "13.00", "113.00", MonedaFE.CRC, BigDecimal.ONE);

            assertThat(service.sincronizarAsientoVenta(factura)).isNull();
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("criterio 7c: montos cambiados → reversión del vigente + asiento nuevo")
        void conCambios_reversionYNuevo() {
            Factura factura = factura(32, "100.00", "13.00", "113.00", MonedaFE.CRC, BigDecimal.ONE);
            AsientoContable vigente = service.generarAsientoVenta(factura);
            vigente.setIdAsiento(700L);
            when(asientoRepository.findAsientosVentaVigentes(32L)).thenReturn(List.of(vigente));
            org.mockito.Mockito.clearInvocations(asientoRepository);

            factura.setSubtotal(new BigDecimal("200.00"));
            factura.setIgv(new BigDecimal("26.00"));
            factura.setTotal(new BigDecimal("226.00"));

            AsientoContable nuevo = service.sincronizarAsientoVenta(factura);

            ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
            verify(asientoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            AsientoContable reversion = captor.getAllValues().stream()
                    .filter(a -> a.getAsientoRevertido() == vigente).findFirst().orElseThrow();
            assertThat(haber(reversion, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("113.00");
            assertThat(nuevo.getAsientoRevertido()).isNull();
            assertThat(debe(nuevo, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("226.00");
            assertThat(haber(nuevo, "VENTA_INGRESOS")).isEqualByComparingTo("200.00");
            assertThat(haber(nuevo, "VENTA_IVA_POR_PAGAR")).isEqualByComparingTo("26.00");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static CuentaContable cuenta(long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre(codigo);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return c;
    }

    static Factura factura(int id, String subtotal, String igv, String total, MonedaFE moneda, BigDecimal tipoCambio) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente Prueba");
        Factura f = new Factura();
        f.setIdFactura(id);
        f.setNumeroFactura("F-" + id);
        f.setCliente(cliente);
        f.setSubtotal(new BigDecimal(subtotal));
        f.setIgv(new BigDecimal(igv));
        f.setTotal(new BigDecimal(total));
        f.setMonedaFE(moneda);
        f.setTipoCambio(tipoCambio);
        return f;
    }

    static Pago pago(Long id, String monto, MetodoPago metodo, EstadoPago estado, Factura factura) {
        Pago p = new Pago();
        p.setIdPago(id);
        p.setNumeroPago("PAG-" + id);
        p.setMonto(new BigDecimal(monto));
        p.setMetodoPago(metodo);
        p.setEstado(estado);
        p.setFactura(factura);
        p.setFechaPago(LocalDateTime.now());
        return p;
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

    /** Saldo (debe - haber) por cuenta sumando todos los asientos dados. */
    static Map<Long, BigDecimal> saldoPorCuenta(AsientoContable... asientos) {
        Map<Long, BigDecimal> saldos = new HashMap<>();
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (AsientoContable a : asientos) {
            lineas.addAll(a.getDetalles());
        }
        for (DetalleAsiento d : lineas) {
            saldos.merge(d.getCuenta().getIdCuenta(), d.getDebe().subtract(d.getHaber()), BigDecimal::add);
        }
        return saldos;
    }
}
