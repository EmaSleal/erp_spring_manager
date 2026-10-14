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
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-012 (ADR-015, ADR-016) — CxP manual contra una base real (H2, JPA): el alta guarda la CxP, su
 * asiento de compra y el saldo del proveedor en una sola transacción, y cualquier rechazo o fallo del
 * asiento no deja nada guardado.
 *
 * The test methods run WITHOUT a surrounding test transaction (NOT_SUPPORTED), so
 * {@link CuentaPorPagarServiceImpl#crear} commits or rolls back its own transaction exactly as in
 * production; the database is then read back in a fresh transaction. Every row is removed in
 * {@link #limpiar()}.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({AsientoContableService.class, CuentaPorPagarServiceImpl.class, TipoCambioService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("CxP manual: asiento de compra en la misma transacción (H2)")
class CuentaPorPagarManualJpaTest {

    private static final LocalDate EMISION = LocalDate.of(2026, 9, 15);

    @Autowired private CuentaPorPagarServiceImpl cxpService;
    @Autowired private AsientoContableService asientoService;
    @Autowired private CuentaPorPagarRepository cxpRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private PagoProveedorRepository pagoRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private Proveedor proveedor;
    private CuentaContable gastoServicios;
    private CuentaContable compras;
    private CuentaContable cuentaPorPagar;
    private CuentaContable ivaCredito;
    private CuentaContable banco;
    private CuentaContable caja;
    private CuentaContable cxcClientes;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            gastoServicios = cuenta("5.1.03.001", TipoCuenta.EGRESO, true, true);
            compras = cuenta("5.1.04.001", TipoCuenta.EGRESO, true, true);
            cuentaPorPagar = cuenta("2.1.05.001", TipoCuenta.PASIVO, true, true);
            ivaCredito = cuenta("1.1.04.001", TipoCuenta.ACTIVO, true, true);
            banco = cuenta("1.1.01.002", TipoCuenta.ACTIVO, true, true);
            caja = cuenta("1.1.01.001", TipoCuenta.ACTIVO, true, true);
            cxcClientes = cuenta("1.1.02.001", TipoCuenta.ACTIVO, true, true);
            param("CPP_CUENTA_COMPRAS", compras);
            param("CPP_CUENTA_POR_PAGAR", cuentaPorPagar);
            param("CPP_IVA_CREDITO_FISCAL", ivaCredito);
            param("CPP_BANCO_TRANSFERENCIA", banco);
            param("COBRO_CAJA_EFECTIVO", caja);
            param("VENTA_CXC_CLIENTES", cxcClientes);
            proveedor = proveedorRepository.save(Proveedor.builder()
                    .cedula("3101123456")
                    .nombre("Servicios Técnicos S.A.")
                    .tipoProveedor(TipoProveedor.values()[0])
                    .saldoPendiente(new BigDecimal("1000.00"))
                    .build());
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            detalleRepository.deleteAllInBatch();
            asientoRepository.deleteAllInBatch();
            pagoRepository.deleteAllInBatch();
            cxpRepository.deleteAllInBatch();
            parametroRepository.deleteAllInBatch();
            cuentaRepository.deleteAllInBatch();
            proveedorRepository.deleteAllInBatch();
        });
    }

    // ── Alta correcta ────────────────────────────────────────────────────────

    @Test
    @DisplayName("CxP manual CRC 100 000 + 13 000 contra una cuenta EGRESO: asiento AUTOMATICO_COMPRA contabilizado, fechado y ligado")
    void crearManualCrc_generaAsientoContabilizado() {
        CuentaPorPagar cpp = cxpService.crear(dto("100000.00", "13000.00", null, gastoServicios.getIdCuenta()));

        tx.executeWithoutResult(s -> {
            List<AsientoContable> asientos = asientoRepository.findAll();
            assertThat(asientos).hasSize(1);
            AsientoContable asiento = asientos.get(0);
            assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_COMPRA);
            assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(asiento.getFecha()).isEqualTo(EMISION);
            assertThat(asiento.getCuentaPorPagar().getId()).isEqualTo(cpp.getId());
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(gastoServicios.getIdCuenta())).isEqualByComparingTo("100000.00");
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("13000.00");
            assertThat(saldos.get(cuentaPorPagar.getIdCuenta())).isEqualByComparingTo("-113000.00");

            CuentaPorPagar guardada = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(guardada.getMonto()).isEqualByComparingTo("113000.00");
            assertThat(guardada.getSubtotal()).isEqualByComparingTo("100000.00");
            assertThat(guardada.getImpuesto()).isEqualByComparingTo("13000.00");
            assertThat(guardada.getCuentaGastoId()).isEqualTo(gastoServicios.getIdCuenta());
        });
        assertThat(saldoProveedor()).isEqualByComparingTo("114000.00");
    }

    @Test
    @DisplayName("pago completo de una CxP manual: la CxP queda en 0 en CPP_CUENTA_POR_PAGAR y el gasto refleja el subtotal")
    void pagoCompletoCxpManual_saldaCuentaPorPagar() {
        CuentaPorPagar cpp = cxpService.crear(dto("100000.00", "13000.00", "113000.00", gastoServicios.getIdCuenta()));
        PagoProveedor pago = pagoRepository.save(PagoProveedor.builder()
                .numero("PP-2026-00001")
                .cuentaPorPagar(cpp)
                .monto(new BigDecimal("113000.00"))
                .moneda(MonedaFE.CRC)
                .tipoCambio(BigDecimal.ONE)
                .montoAplicado(new BigDecimal("113000.00"))
                .metodoPago(MetodoPagoProveedor.TRANSFERENCIA)
                .fecha(EMISION.plusDays(10))
                .build());

        asientoService.generarAsientoPagoProveedor(pago);

        tx.executeWithoutResult(s -> {
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(cuentaPorPagar.getIdCuenta())).isEqualByComparingTo("0.00");
            assertThat(saldos.get(gastoServicios.getIdCuenta())).isEqualByComparingTo("100000.00");
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("13000.00");
            assertThat(saldos.get(banco.getIdCuenta())).isEqualByComparingTo("-113000.00");
        });
    }

    // ── Rechazos: no queda nada guardado ─────────────────────────────────────

    @Test
    @DisplayName("rechazo: subtotal + impuesto ≠ monto")
    void rechazo_sumaDistintaDelMonto() {
        assertRechazoSinCambios(dto("100000.00", "13000.00", "113000.01", gastoServicios.getIdCuenta()),
                "no coincide con el monto");
    }

    @Test
    @DisplayName("rechazo: falta cuentaGastoId")
    void rechazo_sinCuentaGasto() {
        assertRechazoSinCambios(dto("100000.00", "13000.00", null, null), "cuentaGastoId");
    }

    @Test
    @DisplayName("rechazo: cuenta inactiva")
    void rechazo_cuentaInactiva() {
        CuentaContable inactiva = tx.execute(s -> cuenta("5.1.09.001", TipoCuenta.EGRESO, false, true));
        assertRechazoSinCambios(dto("100.00", "0.00", null, inactiva.getIdCuenta()), "inactiva");
    }

    @Test
    @DisplayName("rechazo: cuenta que no acepta movimientos")
    void rechazo_cuentaSinMovimientos() {
        CuentaContable agrupadora = tx.execute(s -> cuenta("5.1.09", TipoCuenta.EGRESO, true, false));
        assertRechazoSinCambios(dto("100.00", "0.00", null, agrupadora.getIdCuenta()), "no acepta movimientos");
    }

    @Test
    @DisplayName("rechazo: cuenta de tipo PASIVO")
    void rechazo_cuentaPasivo() {
        CuentaContable pasivo = tx.execute(s -> cuenta("2.1.09.001", TipoCuenta.PASIVO, true, true));
        assertRechazoSinCambios(dto("100.00", "0.00", null, pasivo.getIdCuenta()), "PASIVO");
    }

    @Test
    @DisplayName("rechazo: cuenta de tipo CAPITAL")
    void rechazo_cuentaCapital() {
        CuentaContable capital = tx.execute(s -> cuenta("3.1.01.001", TipoCuenta.CAPITAL, true, true));
        assertRechazoSinCambios(dto("100.00", "0.00", null, capital.getIdCuenta()), "CAPITAL");
    }

    @Test
    @DisplayName("rechazo: cuenta de tipo INGRESO")
    void rechazo_cuentaIngreso() {
        CuentaContable ingreso = tx.execute(s -> cuenta("4.1.01.001", TipoCuenta.INGRESO, true, true));
        assertRechazoSinCambios(dto("100.00", "0.00", null, ingreso.getIdCuenta()), "INGRESO");
    }

    @Test
    @DisplayName("rechazo: cuenta configurada como banco de pagos a proveedor (CPP_BANCO_*)")
    void rechazo_cuentaBancoCpp() {
        assertRechazoSinCambios(dto("100.00", "0.00", null, banco.getIdCuenta()), "CPP_BANCO_TRANSFERENCIA");
    }

    @Test
    @DisplayName("rechazo: cuenta configurada como caja de cobros (COBRO_CAJA_*)")
    void rechazo_cuentaCajaCobros() {
        assertRechazoSinCambios(dto("100.00", "0.00", null, caja.getIdCuenta()), "COBRO_CAJA_EFECTIVO");
    }

    @Test
    @DisplayName("rechazo: cuenta configurada como cuentas por cobrar (VENTA_CXC_CLIENTES)")
    void rechazo_cuentaCxc() {
        assertRechazoSinCambios(dto("100.00", "0.00", null, cxcClientes.getIdCuenta()), "VENTA_CXC_CLIENTES");
    }

    @Test
    @DisplayName("rechazo: cuenta configurada como cuentas por pagar (CPP_CUENTA_POR_PAGAR), aunque sea de tipo ACTIVO")
    void rechazo_cuentaCxp() {
        // The rule is by configuration, not only by type: point CPP_CUENTA_POR_PAGAR to an ACTIVO account
        CuentaContable cxpComoActivo = tx.execute(s -> {
            CuentaContable c = cuenta("1.1.09.001", TipoCuenta.ACTIVO, true, true);
            ParametroContable p = parametroRepository.findByClave("CPP_CUENTA_POR_PAGAR").orElseThrow();
            p.setCuentaContable(c);
            parametroRepository.save(p);
            return c;
        });
        assertRechazoSinCambios(dto("100.00", "0.00", null, cxpComoActivo.getIdCuenta()), "CPP_CUENTA_POR_PAGAR");
    }

    @Test
    @DisplayName("rechazo: la cuenta de proveedores real (PASIVO) tampoco es elegible")
    void rechazo_cuentaCxpPasivo() {
        assertRechazoSinCambios(dto("100.00", "0.00", null, cuentaPorPagar.getIdCuenta()), "PASIVO");
    }

    @Test
    @DisplayName("rechazo: cuenta configurada como IVA crédito fiscal (CPP_IVA_CREDITO_FISCAL)")
    void rechazo_cuentaIvaCredito() {
        assertRechazoSinCambios(dto("100.00", "0.00", null, ivaCredito.getIdCuenta()), "CPP_IVA_CREDITO_FISCAL");
    }

    @Test
    @DisplayName("si falla el asiento (falta CPP_CUENTA_POR_PAGAR) se revierte la CxP ya insertada y el saldo del proveedor no cambia")
    void falloDelAsiento_revierteCxp() {
        tx.executeWithoutResult(s -> parametroRepository.delete(
                parametroRepository.findByClave("CPP_CUENTA_POR_PAGAR").orElseThrow()));

        assertRechazoSinCambios(dto("100000.00", "13000.00", null, gastoServicios.getIdCuenta()),
                "CPP_CUENTA_POR_PAGAR");
    }

    // ── Cuentas elegibles como contrapartida ─────────────────────────────────

    @Test
    @DisplayName("cuentas elegibles: activas, con movimientos, EGRESO o ACTIVO, sin bancos/caja/CxC/CxP/IVA, por código")
    void cuentasElegibles_aplicanLasReglas() {
        tx.executeWithoutResult(s -> {
            cuenta("5.1.09.001", TipoCuenta.EGRESO, false, true);   // inactiva
            cuenta("5.1.09", TipoCuenta.EGRESO, true, false);        // no acepta movimientos
            cuenta("4.1.01.001", TipoCuenta.INGRESO, true, true);
            cuenta("3.1.01.001", TipoCuenta.CAPITAL, true, true);
            cuenta("1.2.01.001", TipoCuenta.ACTIVO, true, true);     // activo fijo: elegible
        });

        List<CuentaContable> elegibles = tx.execute(s -> asientoService.obtenerCuentasContrapartidaCxpManual());

        assertThat(elegibles).extracting(CuentaContable::getCodigo)
                .containsExactly("1.2.01.001", "5.1.03.001", "5.1.04.001");
        Optional<Long> idCompras = tx.execute(s -> asientoService.obtenerIdCuentaComprasCpp());
        assertThat(idCompras).contains(compras.getIdCuenta());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void assertRechazoSinCambios(CuentaPorPagarDTO dto, String mensaje) {
        BigDecimal saldoAntes = saldoProveedor();

        assertThatThrownBy(() -> cxpService.crear(dto))
                .isInstanceOfAny(IllegalArgumentException.class, IllegalStateException.class)
                .hasMessageContaining(mensaje);

        assertThat(cxpRepository.count()).isZero();
        assertThat(asientoRepository.count()).isZero();
        assertThat(detalleRepository.count()).isZero();
        assertThat(saldoProveedor()).isEqualByComparingTo(saldoAntes);
    }

    private BigDecimal saldoProveedor() {
        return proveedorRepository.findById(proveedor.getId()).orElseThrow().getSaldoPendiente();
    }

    /** DR − CR por cuenta, sobre todos los asientos contabilizados. */
    private Map<Long, BigDecimal> saldosPorCuenta() {
        Map<Long, BigDecimal> saldos = new HashMap<>();
        for (DetalleAsiento d : detalleRepository.findAll()) {
            if (d.getAsiento().getEstado() != EstadoAsiento.CONTABILIZADO) {
                continue;
            }
            saldos.merge(d.getCuenta().getIdCuenta(), d.getDebe().subtract(d.getHaber()), BigDecimal::add);
        }
        return saldos;
    }

    private CuentaPorPagarDTO dto(String subtotal, String impuesto, String monto, Long cuentaGastoId) {
        return CuentaPorPagarDTO.builder()
                .proveedorId(proveedor.getId())
                .descripcion("Mantenimiento de equipos")
                .subtotal(new BigDecimal(subtotal))
                .impuesto(new BigDecimal(impuesto))
                .monto(monto != null ? new BigDecimal(monto) : null)
                .cuentaGastoId(cuentaGastoId)
                .moneda(MonedaFE.CRC)
                .fechaEmision(EMISION)
                .build();
    }

    private CuentaContable cuenta(String codigo, TipoCuenta tipo, boolean activa, boolean aceptaMovimientos) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setTipo(tipo);
        c.setNivel(4);
        c.setActiva(activa);
        c.setAceptaMovimientos(aceptaMovimientos);
        return cuentaRepository.save(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
