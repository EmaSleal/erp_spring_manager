package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.CuentaBancariaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
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
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.NumeroPagoGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.PagoServiceImpl;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.impl.PagoProveedorServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * T-015 (ADR-018) — cobros y pagos a proveedor contra la hoja contable de su cuenta bancaria, con los
 * servicios reales sobre H2 (JPA).
 *
 * Fixture: el árbol de V30 — 1.1.01.002 Bancos agrupadora con una hoja por cuenta bancaria
 * (BN CRC .001, BN USD .002, BAC CRC .003, BAC USD .004) — más caja, CxC, anticipos, CxP y
 * diferencial. Sin claves COBRO_CAJA_TRANSFERENCIA ni CPP_BANCO_* bancarias (V30 las elimina).
 *
 * Los métodos corren SIN transacción de prueba (NOT_SUPPORTED): cada servicio confirma o revierte
 * su propia transacción como en producción y la base se relee en una transacción nueva.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({AsientoContableService.class, TipoCambioService.class, CuentaBancariaService.class,
        PagoServiceImpl.class, NumeroPagoGeneratorService.class, PagoMapper.class, FacturaMapper.class,
        PagoProveedorServiceImpl.class, CuentaPorPagarServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-015 — cobros y pagos a proveedor contra la hoja de su cuenta bancaria (H2)")
class CuentasBancariasAsientosJpaTest {

    private static final LocalDateTime FECHA_COBRO = LocalDateTime.of(2026, 10, 1, 10, 0);
    private static final LocalDate FECHA_PAGO = LocalDate.of(2026, 10, 2);

    @MockitoBean private EmpresaService empresaService;

    @Autowired private PagoServiceImpl pagoService;
    @Autowired private PagoProveedorServiceImpl pagoProveedorService;
    @Autowired private CuentaBancariaService cuentaBancariaService;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private CuentaBancariaRepository cuentaBancariaRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private CuentaPorPagarRepository cuentaPorPagarRepository;
    @Autowired private PagoProveedorRepository pagoProveedorRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    private CuentaContable caja;
    private CuentaContable cxc;
    private CuentaContable cxp;
    private CuentaContable diferencialGasto;
    private CuentaBancaria bnCrc;
    private CuentaBancaria bnUsd;
    private CuentaBancaria bacCrc;
    private CuentaBancaria bacUsd;
    private Cliente cliente;
    private Factura factura;
    private Proveedor proveedor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            Empresa empresa = new Empresa();
            empresa.setNombreEmpresa("Empresa de prueba S.A.");
            empresa.setActivo(true);
            empresa = empresaRepository.save(empresa);
            when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);

            caja = cuenta("1.1.01.001", "Caja General", TipoCuenta.ACTIVO, null, true);
            CuentaContable bancos = cuenta("1.1.01.002", "Bancos", TipoCuenta.ACTIVO, null, false);
            bnCrc = banco(empresa, "Banco Nacional - Colones", "CRC", cuenta("1.1.01.002.001",
                    "Banco Nacional - Colones CRC", TipoCuenta.ACTIVO, bancos, true));
            bnUsd = banco(empresa, "Banco Nacional - Dólares", "USD", cuenta("1.1.01.002.002",
                    "Banco Nacional - Dólares USD", TipoCuenta.ACTIVO, bancos, true));
            bacCrc = banco(empresa, "BAC San José - Colones", "CRC", cuenta("1.1.01.002.003",
                    "BAC San José - Colones CRC", TipoCuenta.ACTIVO, bancos, true));
            bacUsd = banco(empresa, "BAC San José - Dólares", "USD", cuenta("1.1.01.002.004",
                    "BAC San José - Dólares USD", TipoCuenta.ACTIVO, bancos, true));
            cxc = cuenta("1.1.02.001", "Clientes", TipoCuenta.ACTIVO, null, true);
            CuentaContable anticipos = cuenta("2.1.03.001", "Anticipos de clientes", TipoCuenta.PASIVO, null, true);
            cxp = cuenta("2.1.01.001", "Proveedores", TipoCuenta.PASIVO, null, true);
            CuentaContable diferencialIngreso = cuenta("4.2.01.001", "Diferencial ingreso", TipoCuenta.INGRESO, null, true);
            diferencialGasto = cuenta("5.2.01.001", "Diferencial gasto", TipoCuenta.EGRESO, null, true);

            param("COBRO_CAJA_EFECTIVO", caja);
            param("CPP_BANCO_EFECTIVO", caja);
            param("VENTA_CXC_CLIENTES", cxc);
            param("COBRO_ANTICIPOS_CLIENTES", anticipos);
            param("CPP_CUENTA_POR_PAGAR", cxp);
            param("DIFERENCIAL_CAMBIARIO_INGRESO", diferencialIngreso);
            param("DIFERENCIAL_CAMBIARIO_GASTO", diferencialGasto);

            cliente = new Cliente();
            cliente.setNombre("Cliente de prueba");
            cliente = clienteRepository.save(cliente);
            factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("FAC-T015-1");
            factura.setSubtotal(new BigDecimal("100000.00"));
            factura.setTotal(new BigDecimal("113000.00"));
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setAnulada(false);
            factura = facturaRepository.save(factura);

            proveedor = proveedorRepository.save(Proveedor.builder()
                    .cedula("3101999999").nombre("Proveedor de prueba")
                    .tipoProveedor(TipoProveedor.values()[0])
                    .saldoPendiente(BigDecimal.ZERO).build());
            usuario = new Usuario();
            usuario.setNombre("Tesorero");
            usuario.setEmail("tesorero@t015.test");
            usuario = usuarioRepository.save(usuario);
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            detalleRepository.deleteAllInBatch();
            asientoRepository.findAll().forEach(a -> a.setAsientoRevertido(null));
            asientoRepository.flush();
            asientoRepository.deleteAllInBatch();
            pagoRepository.deleteAllInBatch();
            pagoProveedorRepository.deleteAllInBatch();
            cuentaPorPagarRepository.deleteAllInBatch();
            proveedorRepository.deleteAllInBatch();
            facturaRepository.deleteAllInBatch();
            clienteRepository.deleteAllInBatch();
            cuentaBancariaRepository.deleteAllInBatch();
            parametroRepository.deleteAllInBatch();
            cuentaRepository.findAll().forEach(c -> c.setCuentaPadre(null));
            cuentaRepository.flush();
            cuentaRepository.deleteAllInBatch();
            empresaRepository.deleteAllInBatch();
            usuarioRepository.deleteAllInBatch();
        });
    }

    // ── Cobros ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cobro TRANSFERENCIA CRC a Banco Nacional - Colones: DR hoja de BN CRC / CR CxC")
    void cobroTransferenciaCrc_debitaHojaDelBanco() {
        Pago pago = pagoService.registrarPago(cobro("100000.00", MonedaFE.CRC, MetodoPago.TRANSFERENCIA,
                bnCrc.getIdCuentaBancaria(), factura.getIdFactura()));

        tx.executeWithoutResult(s -> {
            Pago guardado = pagoRepository.findById(pago.getIdPago()).orElseThrow();
            assertThat(guardado.getCuentaBancariaDestino().getIdCuentaBancaria()).isEqualTo(bnCrc.getIdCuentaBancaria());
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(hoja(bnCrc))).isEqualByComparingTo("100000.00");
            assertThat(saldos.get(cxc.getIdCuenta())).isEqualByComparingTo("-100000.00");
            assertThat(saldos).hasSize(2);
        });
    }

    @Test
    @DisplayName("cobro EFECTIVO debita 1.1.01.001 (COBRO_CAJA_EFECTIVO) e ignora una cuenta bancaria enviada")
    void cobroEfectivo_debitaCaja() {
        Pago pago = pagoService.registrarPago(cobro("25000.00", MonedaFE.CRC, MetodoPago.EFECTIVO,
                bnCrc.getIdCuentaBancaria(), factura.getIdFactura()));

        tx.executeWithoutResult(s -> {
            assertThat(pagoRepository.findById(pago.getIdPago()).orElseThrow().getCuentaBancariaDestino()).isNull();
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(caja.getIdCuenta())).isEqualByComparingTo("25000.00");
            assertThat(saldos.get(cxc.getIdCuenta())).isEqualByComparingTo("-25000.00");
            assertThat(saldos.get(hoja(bnCrc))).isNull();
        });
    }

    @Test
    @DisplayName("cobro TRANSFERENCIA sin cuenta bancaria: rechazado, no se guarda nada")
    void cobroTransferenciaSinCuenta_rechazado() {
        assertThatThrownBy(() -> pagoService.registrarPago(cobro("1000.00", MonedaFE.CRC, MetodoPago.TRANSFERENCIA,
                null, factura.getIdFactura())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuenta bancaria de destino");
        assertNadaGuardado();
    }

    @Test
    @DisplayName("cobro USD (adelanto) con cuenta bancaria en CRC: rechazado, no se guarda nada")
    void cobroUsdConCuentaCrc_rechazado() {
        PagoDTO dto = cobro("100.00", MonedaFE.USD, MetodoPago.TRANSFERENCIA, bnCrc.getIdCuentaBancaria(), null);
        dto.setTipoPago(TipoPago.ADELANTO);
        dto.setTipoCambio(new BigDecimal("510"));

        assertThatThrownBy(() -> pagoService.registrarPago(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("está en CRC").hasMessageContaining("pago en USD");
        assertNadaGuardado();
    }

    @Test
    @DisplayName("cobro a una cuenta bancaria inactiva: rechazado, no se guarda nada")
    void cobroCuentaInactiva_rechazado() {
        cuentaBancariaService.cambiarEstado(bacCrc.getIdCuentaBancaria(), false);

        assertThatThrownBy(() -> pagoService.registrarPago(cobro("1000.00", MonedaFE.CRC, MetodoPago.TRANSFERENCIA,
                bacCrc.getIdCuentaBancaria(), factura.getIdFactura())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactiva");
        assertNadaGuardado();
    }

    // ── Pagos a proveedor ────────────────────────────────────────────────────

    @Test
    @DisplayName("pago SINPE USD desde BAC San José - Dólares: CR hoja BAC USD por monto × tc (100 × 510 = 51 000)")
    void pagoSinpeUsd_acreditaHojaBacUsd() {
        CuentaPorPagar cpp = cxp("CPP-T015-USD", "200.00", MonedaFE.USD, "500");

        pagoProveedorService.registrarPago(pagoProveedor(cpp, "100.00", MonedaFE.USD, "510",
                MetodoPagoProveedor.SINPE, bacUsd.getIdCuentaBancaria()), usuario);

        tx.executeWithoutResult(s -> {
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(hoja(bacUsd))).isEqualByComparingTo("-51000.00");
            assertThat(saldos.get(cxp.getIdCuenta())).isEqualByComparingTo("50000.00");
            assertThat(saldos.get(diferencialGasto.getIdCuenta())).isEqualByComparingTo("1000.00");
            PagoProveedor guardado = pagoProveedorRepository.findAll().get(0);
            assertThat(guardado.getCuentaBancariaOrigen().getIdCuentaBancaria()).isEqualTo(bacUsd.getIdCuentaBancaria());
        });
    }

    @Test
    @DisplayName("pago a proveedor EFECTIVO acredita 1.1.01.001 (CPP_BANCO_EFECTIVO) sin cuenta bancaria")
    void pagoEfectivo_acreditaCaja() {
        CuentaPorPagar cpp = cxp("CPP-T015-CRC", "50000.00", MonedaFE.CRC, "1");

        pagoProveedorService.registrarPago(pagoProveedor(cpp, "20000.00", MonedaFE.CRC, null,
                MetodoPagoProveedor.EFECTIVO, null), usuario);

        tx.executeWithoutResult(s -> {
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(caja.getIdCuenta())).isEqualByComparingTo("-20000.00");
            assertThat(saldos.get(cxp.getIdCuenta())).isEqualByComparingTo("20000.00");
            assertThat(pagoProveedorRepository.findAll().get(0).getCuentaBancariaOrigen()).isNull();
        });
    }

    @Test
    @DisplayName("pago a proveedor CHEQUE sin cuenta bancaria: rechazado, no se guarda nada y el saldo no cambia")
    void pagoChequeSinCuenta_rechazado() {
        CuentaPorPagar cpp = cxp("CPP-T015-CRC", "50000.00", MonedaFE.CRC, "1");

        assertThatThrownBy(() -> pagoProveedorService.registrarPago(pagoProveedor(cpp, "20000.00", MonedaFE.CRC, null,
                MetodoPagoProveedor.CHEQUE, null), usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuenta bancaria de origen");
        assertNadaGuardadoProveedor(cpp);
    }

    @Test
    @DisplayName("pago a proveedor en CRC desde una cuenta bancaria en USD: rechazado, no se guarda nada")
    void pagoMonedaDistinta_rechazado() {
        CuentaPorPagar cpp = cxp("CPP-T015-CRC", "50000.00", MonedaFE.CRC, "1");

        assertThatThrownBy(() -> pagoProveedorService.registrarPago(pagoProveedor(cpp, "20000.00", MonedaFE.CRC, null,
                MetodoPagoProveedor.TRANSFERENCIA, bnUsd.getIdCuentaBancaria()), usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("está en USD").hasMessageContaining("pago en CRC");
        assertNadaGuardadoProveedor(cpp);
    }

    // ── Conciliación y anulación ─────────────────────────────────────────────

    @Test
    @DisplayName("conciliación: cobro 100 000 a BN CRC y pago 40 000 desde BN CRC → hoja BN CRC = 60 000; las otras 3 hojas = 0")
    void conciliacion_saldoDeLaHojaBancaria() {
        CuentaPorPagar cpp = cxp("CPP-T015-CRC", "50000.00", MonedaFE.CRC, "1");

        pagoService.registrarPago(cobro("100000.00", MonedaFE.CRC, MetodoPago.TRANSFERENCIA,
                bnCrc.getIdCuentaBancaria(), factura.getIdFactura()));
        pagoProveedorService.registrarPago(pagoProveedor(cpp, "40000.00", MonedaFE.CRC, null,
                MetodoPagoProveedor.TRANSFERENCIA, bnCrc.getIdCuentaBancaria()), usuario);

        tx.executeWithoutResult(s -> {
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(hoja(bnCrc))).isEqualByComparingTo("60000.00");
            for (CuentaBancaria otra : List.of(bnUsd, bacCrc, bacUsd)) {
                assertThat(saldos.getOrDefault(hoja(otra), BigDecimal.ZERO)).as(otra.getEntidad())
                        .isEqualByComparingTo("0");
            }
        });
    }

    @Test
    @DisplayName("anular un cobro y un pago a proveedor con cuenta bancaria: la reversión deja la hoja del banco en 0")
    void anulacion_dejaLaHojaEnCero() {
        CuentaPorPagar cpp = cxp("CPP-T015-CRC", "50000.00", MonedaFE.CRC, "1");
        Pago cobro = pagoService.registrarPago(cobro("100000.00", MonedaFE.CRC, MetodoPago.TRANSFERENCIA,
                bacCrc.getIdCuentaBancaria(), factura.getIdFactura()));
        PagoProveedor pago = pagoProveedorService.registrarPago(pagoProveedor(cpp, "40000.00", MonedaFE.CRC, null,
                MetodoPagoProveedor.DEPOSITO, bacCrc.getIdCuentaBancaria()), usuario);
        BigDecimal saldoAntes = tx.execute(s -> saldosPorCuenta().get(hoja(bacCrc)));
        assertThat(saldoAntes).isEqualByComparingTo("60000.00");

        pagoService.anularPago(cobro.getIdPago(), "Transferencia devuelta", usuario.getIdUsuario());
        pagoProveedorService.anularPago(pago.getId(), usuario);

        tx.executeWithoutResult(s -> {
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(hoja(bacCrc))).isEqualByComparingTo("0");
            assertThat(saldos.get(cxc.getIdCuenta())).isEqualByComparingTo("0");
            assertThat(saldos.get(cxp.getIdCuenta())).isEqualByComparingTo("0");
            // Original + reversión de cada pago, todos contabilizados (el original no se edita)
            assertThat(asientoRepository.findAll()).hasSize(4)
                    .allMatch(a -> a.getEstado() == EstadoAsiento.CONTABILIZADO);
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertNadaGuardado() {
        tx.executeWithoutResult(s -> {
            assertThat(pagoRepository.count()).isZero();
            assertThat(asientoRepository.count()).isZero();
            assertThat(detalleRepository.count()).isZero();
        });
    }

    private void assertNadaGuardadoProveedor(CuentaPorPagar cpp) {
        tx.executeWithoutResult(s -> {
            assertThat(pagoProveedorRepository.count()).isZero();
            assertThat(asientoRepository.count()).isZero();
            CuentaPorPagar releida = cuentaPorPagarRepository.findById(cpp.getId()).orElseThrow();
            assertThat(releida.getSaldoPendiente()).isEqualByComparingTo(cpp.getSaldoPendiente());
        });
    }

    private Long hoja(CuentaBancaria banco) {
        return banco.getCuentaContable().getIdCuenta();
    }

    private Map<Long, BigDecimal> saldosPorCuenta() {
        Map<Long, BigDecimal> saldos = new HashMap<>();
        for (DetalleAsiento d : detalleRepository.findAll()) {
            AsientoContable asiento = d.getAsiento();
            if (asiento.getEstado() != EstadoAsiento.CONTABILIZADO) {
                continue;
            }
            saldos.merge(d.getCuenta().getIdCuenta(), d.getDebe().subtract(d.getHaber()), BigDecimal::add);
        }
        return saldos;
    }

    private PagoDTO cobro(String monto, MonedaFE moneda, MetodoPago metodo, Integer idCuentaBancaria, Integer idFactura) {
        return PagoDTO.builder()
                .idCliente(cliente.getIdCliente())
                .idFactura(idFactura)
                .tipoPago(idFactura != null ? TipoPago.PARCIAL : TipoPago.ADELANTO)
                .monto(new BigDecimal(monto))
                .moneda(moneda)
                .metodoPago(metodo)
                .fechaPago(FECHA_COBRO)
                .referenciaBancaria(metodo.requiereReferencia() ? "REF-" + monto + "-" + System.nanoTime() : null)
                .estado(EstadoPago.CONFIRMADO)
                .idCuentaBancariaDestino(idCuentaBancaria)
                .build();
    }

    private PagoProveedorDTO pagoProveedor(CuentaPorPagar cpp, String monto, MonedaFE moneda, String tipoCambio,
                                           MetodoPagoProveedor metodo, Integer idCuentaBancaria) {
        return PagoProveedorDTO.builder()
                .cuentaPorPagarId(cpp.getId())
                .monto(new BigDecimal(monto))
                .moneda(moneda)
                .tipoCambio(tipoCambio != null ? new BigDecimal(tipoCambio) : null)
                .metodoPago(metodo)
                .idCuentaBancariaOrigen(idCuentaBancaria)
                .referencia("REF-PP")
                .fecha(FECHA_PAGO)
                .build();
    }

    private CuentaPorPagar cxp(String numero, String monto, MonedaFE moneda, String tipoCambio) {
        return tx.execute(s -> cuentaPorPagarRepository.save(CuentaPorPagar.builder()
                .numero(numero)
                .proveedor(proveedor)
                .descripcion("Compra de prueba")
                .monto(new BigDecimal(monto))
                .saldoPendiente(new BigDecimal(monto))
                .moneda(moneda)
                .tipoCambio(new BigDecimal(tipoCambio))
                .fechaEmision(LocalDate.of(2026, 9, 15))
                .estado(EstadoCuentaPorPagar.PENDIENTE)
                .build()));
    }

    private CuentaContable cuenta(String codigo, String nombre, TipoCuenta tipo, CuentaContable padre, boolean acepta) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setNaturaleza(NaturalezaCuenta.fromTipoCuenta(tipo));
        c.setNivel(codigo.split("\\.").length);
        c.setActiva(true);
        c.setAceptaMovimientos(acepta);
        c.setCuentaPadre(padre);
        return cuentaRepository.save(c);
    }

    private CuentaBancaria banco(Empresa empresa, String entidad, String moneda, CuentaContable hoja) {
        CuentaBancaria b = new CuentaBancaria();
        b.setEmpresa(empresa);
        b.setEntidad(entidad);
        b.setMoneda(moneda);
        b.setActiva(true);
        b.setOrden(0);
        b.setCuentaContable(hoja);
        return cuentaBancariaRepository.save(b);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
