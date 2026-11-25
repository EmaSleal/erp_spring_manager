package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.config.ClockConfig;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
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
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-052 (ADR-066 §1-3, ADR-014, ADR-023): datos del comprobante del proveedor, bloqueo de duplicados, anulación con
 * reversión contable y saldo del proveedor en CRC, contra una base real (H2, JPA). Las pruebas corren sin transacción
 * envolvente para que el servicio confirme o revierta la suya como en producción.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({AsientoContableService.class, CuentaPorPagarServiceImpl.class, TipoCambioService.class,
        ComprobanteProveedorValidator.class, ClockConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-052 — CxP: comprobante del proveedor, duplicados, anulación y saldo en CRC (H2)")
class CuentaPorPagarComprobanteJpaTest {

    private static final LocalDate COMPROBANTE = LocalDate.of(2026, 9, 15);

    @Autowired private CuentaPorPagarServiceImpl cxpService;
    @Autowired private CuentaPorPagarRepository cxpRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private PagoProveedorRepository pagoRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private UsuarioActividadService actividadService;
    @MockitoBean private api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService topeComprasRts;

    private TransactionTemplate tx;
    private Proveedor proveedor;
    private Proveedor otroProveedor;
    private CuentaContable gasto;
    private CuentaContable ivaCredito;
    private CuentaContable cuentaPorPagar;
    private CuentaContable compras;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(7);
        tx.executeWithoutResult(s -> {
            gasto = cuenta("5.1.03.001", TipoCuenta.EGRESO);
            compras = cuenta("5.1.04.001", TipoCuenta.EGRESO);
            cuentaPorPagar = cuenta("2.1.05.001", TipoCuenta.PASIVO);
            ivaCredito = cuenta("1.1.04.001", TipoCuenta.ACTIVO);
            param("CPP_CUENTA_COMPRAS", compras);
            param("CPP_CUENTA_POR_PAGAR", cuentaPorPagar);
            param("CPP_IVA_CREDITO_FISCAL", ivaCredito);
            proveedor = proveedorRepository.save(Proveedor.builder().cedula("3101123456").nombre("Servicios S.A.")
                    .tipoProveedor(TipoProveedor.values()[0]).build());
            otroProveedor = proveedorRepository.save(Proveedor.builder().cedula("3101999999").nombre("Otro S.A.")
                    .tipoProveedor(TipoProveedor.values()[0]).build());
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            cuentaRepository.findAll().forEach(c -> c.setCuentaPadre(null));
            cuentaRepository.flush();
            detalleRepository.deleteAllInBatch();
            asientoRepository.deleteAllInBatch();
            pagoRepository.deleteAllInBatch();
            cxpRepository.findAll().forEach(c -> c.getLineas().clear());
            cxpRepository.flush();
            cxpRepository.deleteAllInBatch();
            ordenCompraRepository.deleteAllInBatch();
            parametroRepository.deleteAllInBatch();
            cuentaRepository.deleteAllInBatch();
            proveedorRepository.deleteAllInBatch();
        });
    }

    // ── Datos del comprobante y fecha del asiento ────────────────────────────

    @Test
    @DisplayName("crear guarda número, clave, fecha y desglose; la fecha de la CxP y del asiento es la del comprobante, no la de registro")
    void crear_guardaComprobanteYFechaEsLaDelComprobante() {
        CuentaPorPagarDTO dto = dto("FE-0001", "100000.00", "13000.00");
        dto.setClaveComprobante("1".repeat(50));
        dto.setFechaEmision(LocalDate.of(2026, 1, 1)); // se ignora: manda la fecha del comprobante

        CuentaPorPagar cpp = cxpService.crear(dto);

        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getNumeroComprobanteProveedor()).isEqualTo("FE-0001");
            assertThat(g.getClaveComprobante()).isEqualTo("1".repeat(50));
            assertThat(g.getFechaComprobante()).isEqualTo(COMPROBANTE);
            assertThat(g.getFechaEmision()).isEqualTo(COMPROBANTE);
            assertThat(g.getLineas()).hasSize(1);
            assertThat(g.getLineas().get(0).getCodigoTarifa()).isEqualTo("08");
            assertThat(g.getLineas().get(0).getBase()).isEqualByComparingTo("100000.00");
            assertThat(g.getLineas().get(0).getIva()).isEqualByComparingTo("13000.00");
            List<AsientoContable> asientos = asientoRepository.findAll();
            assertThat(asientos).hasSize(1);
            assertThat(asientos.get(0).getFecha()).isEqualTo(COMPROBANTE);
        });
    }

    @Test
    @DisplayName("sin número o sin líneas la CxP manual se rechaza y no queda nada guardado")
    void crear_rechazaComprobanteInvalido() {
        CuentaPorPagarDTO sinNumero = dto(null, "100000.00", "13000.00");
        assertThatThrownBy(() -> cxpService.crear(sinNumero)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ComprobanteProveedorValidator.MSG_NUMERO);
        CuentaPorPagarDTO sinLineas = dto("FE-0002", "100000.00", "13000.00");
        sinLineas.setLineas(List.of());
        assertThatThrownBy(() -> cxpService.crear(sinLineas)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ComprobanteProveedorValidator.MSG_LINEAS);
        assertThat(cxpRepository.count()).isZero();
        assertThat(asientoRepository.count()).isZero();
    }

    // ── Duplicados ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("el mismo proveedor no puede repetir el comprobante (sin espacios ni mayúsculas): 409 con mensaje fijo y nada guardado")
    void duplicado_mismoProveedor() {
        cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));
        long asientos = asientoRepository.count();

        assertThatThrownBy(() -> cxpService.crear(dto(" fe - 0001 ", "50000.00", "6500.00")))
                .isInstanceOf(ComprobanteProveedorDuplicadoException.class)
                .hasMessage("Ya existe una cuenta por pagar de este proveedor con el comprobante fe - 0001");

        assertThat(cxpRepository.count()).isEqualTo(1);
        assertThat(asientoRepository.count()).isEqualTo(asientos);
        assertThat(saldo(proveedor)).isEqualByComparingTo("113000.00");
    }

    @Test
    @DisplayName("otro proveedor sí puede usar el mismo número de comprobante")
    void duplicado_otroProveedorPermitido() {
        cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));
        CuentaPorPagarDTO otro = dto("FE-0001", "100000.00", "13000.00");
        otro.setProveedorId(otroProveedor.getId());

        assertThat(cxpService.crear(otro).getId()).isNotNull();
        assertThat(cxpRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("una CxP anulada no bloquea volver a registrar el comprobante")
    void duplicado_permitidoTrasAnular() {
        CuentaPorPagar primera = cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));
        cxpService.anular(primera.getId(), "Registrada por error", usuario);

        CuentaPorPagar segunda = cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));

        assertThat(segunda.getId()).isNotEqualTo(primera.getId());
        assertThat(cxpRepository.count()).isEqualTo(2);
        // y ahora sí hay una vigente: no se admite una tercera
        assertThatThrownBy(() -> cxpService.crear(dto("FE-0001", "100000.00", "13000.00")))
                .isInstanceOf(ComprobanteProveedorDuplicadoException.class);
    }

    @Test
    @DisplayName("respaldo en la base: el índice único (proveedor, comprobante normalizado) rechaza el segundo insert aunque se omita el servicio")
    void duplicado_indiceUnicoEnLaBase() {
        tx.executeWithoutResult(s -> cxpRepository.saveAndFlush(cxpDirecta("CPP-T-1", "FE-9", "FE-9")));
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> cxpRepository.saveAndFlush(cxpDirecta("CPP-T-2", "fe-9", "FE-9"))))
                .isInstanceOf(DataIntegrityViolationException.class);
        // sin normalizado (CxP anuladas u originadas en OC) no hay restricción
        tx.executeWithoutResult(s -> cxpRepository.saveAndFlush(cxpDirecta("CPP-T-3", "FE-9", null)));
        tx.executeWithoutResult(s -> cxpRepository.saveAndFlush(cxpDirecta("CPP-T-4", "FE-9", null)));
    }

    // ── Anulación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("anular: pasa a ANULADA con usuario, fecha y motivo; revierte el asiento de compra hoy; recalcula el saldo; audita; no borra nada")
    void anular_ok() {
        CuentaPorPagar cpp = cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));
        assertThat(saldo(proveedor)).isEqualByComparingTo("113000.00");

        cxpService.anular(cpp.getId(), "  Factura registrada por error  ", usuario);

        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getEstado()).isEqualTo(EstadoCuentaPorPagar.ANULADA);
            assertThat(g.getAnuladaPorId()).isEqualTo(7);
            assertThat(g.getFechaAnulacion()).isNotNull();
            assertThat(g.getMotivoAnulacion()).isEqualTo("Factura registrada por error");
            assertThat(g.getLineas()).hasSize(1);

            List<AsientoContable> asientos = asientoRepository.findAll();
            assertThat(asientos).hasSize(2);
            AsientoContable original = asientos.stream().filter(a -> a.getAsientoRevertido() == null).findFirst().orElseThrow();
            AsientoContable reversion = asientos.stream().filter(a -> a.getAsientoRevertido() != null).findFirst().orElseThrow();
            assertThat(original.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_COMPRA);
            assertThat(original.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(reversion.getAsientoRevertido().getIdAsiento()).isEqualTo(original.getIdAsiento());
            assertThat(reversion.getFecha()).isEqualTo(LocalDate.now(new ClockConfig().clock()));
            assertThat(original.getFecha()).isEqualTo(COMPROBANTE);
            saldosPorCuenta().values().forEach(v -> assertThat(v).isEqualByComparingTo("0.00"));
        });
        assertThat(saldo(proveedor)).isEqualByComparingTo("0.00");
        verify(actividadService).registrarActividad(eq(7), anyString(), contains("anulada"), eq("CuentaPorPagar"),
                eq(cpp.getId().intValue()));
    }

    @Test
    @DisplayName("anular rechaza el motivo nulo, corto (<3) o largo (>180) con mensaje fijo y no cambia nada")
    void anular_motivoInvalido() {
        CuentaPorPagar cpp = cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));
        for (String motivo : new String[] {null, "", "  ", "ab", " a ", "x".repeat(181)}) {
            assertThatThrownBy(() -> cxpService.anular(cpp.getId(), motivo, usuario))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(CuentaPorPagarService.MSG_ANULAR_MOTIVO);
        }
        assertThat(cxpService.findById(cpp.getId()).getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(cxpService.anular(cpp.getId(), "x".repeat(180), usuario).getEstado())
                .isEqualTo(EstadoCuentaPorPagar.ANULADA);
    }

    @Test
    @DisplayName("anular con un pago CONFIRMADO: IllegalStateException con mensaje fijo, sin cambios; con el pago ANULADO sí procede")
    void anular_conPagos() {
        CuentaPorPagar cpp = cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));
        PagoProveedor pago = pagoRepository.save(PagoProveedor.builder().numero("PP-2026-00001").cuentaPorPagar(cpp)
                .monto(new BigDecimal("1000.00")).montoAplicado(new BigDecimal("1000.00")).moneda(MonedaFE.CRC)
                .tipoCambio(BigDecimal.ONE).metodoPago(MetodoPagoProveedor.EFECTIVO).fecha(COMPROBANTE).build());

        assertThatThrownBy(() -> cxpService.anular(cpp.getId(), "Error de captura", usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(CuentaPorPagarService.MSG_ANULAR_CON_PAGOS);
        assertThat(asientoRepository.count()).isEqualTo(1);
        assertThat(cxpService.findById(cpp.getId()).getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        verifyNoInteractions(actividadService);

        pago.setEstado(EstadoPagoProveedor.ANULADO);
        pagoRepository.save(pago);
        assertThat(cxpService.anular(cpp.getId(), "Error de captura", usuario).getEstado())
                .isEqualTo(EstadoCuentaPorPagar.ANULADA);
    }

    @Test
    @DisplayName("anular dos veces: la segunda responde con el mensaje fijo de ya anulada y no revierte de nuevo")
    void anular_dobleAnulacion() {
        CuentaPorPagar cpp = cxpService.crear(dto("FE-0001", "100000.00", "13000.00"));
        cxpService.anular(cpp.getId(), "Error de captura", usuario);

        assertThatThrownBy(() -> cxpService.anular(cpp.getId(), "Otra vez", usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(CuentaPorPagarService.MSG_ANULAR_YA_ANULADA);
        assertThat(asientoRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("una CxP heredada sin asiento se anula igual, sin reversión, y la auditoría lo dice")
    void anular_sinAsientoHeredado() {
        CuentaPorPagar legada = cxpRepository.save(cxpDirecta("CPP-2026-00001", null, null));
        assertThat(asientoRepository.count()).isZero();

        cxpService.anular(legada.getId(), "Cuenta heredada", usuario);

        assertThat(cxpService.findById(legada.getId()).getEstado()).isEqualTo(EstadoCuentaPorPagar.ANULADA);
        assertThat(asientoRepository.count()).isZero();
        verify(actividadService).registrarActividad(eq(7), anyString(), contains("sin asiento"), eq("CuentaPorPagar"),
                eq(legada.getId().intValue()));
    }

    @Test
    @DisplayName("una CxP de orden de compra conserva el enlace y la anulación no cambia el estado de la orden")
    void anular_conOrdenCompra() {
        OrdenCompra oc = ordenCompraRepository.save(OrdenCompra.builder().numero("OC-2026-00001").proveedor(proveedor)
                .estado(EstadoOrdenCompra.RECIBIDA).fecha(COMPROBANTE).fechaRecepcion(COMPROBANTE).moneda(MonedaFE.CRC)
                .subtotal(new BigDecimal("1000.00")).impuesto(new BigDecimal("130.00"))
                .total(new BigDecimal("1130.00")).build());
        CuentaPorPagar cpp = cxpService.crear(CuentaPorPagarDTO.builder().ordenCompraId(oc.getId())
                .descripcion("Factura de la OC").numeroComprobanteProveedor("FE-OC-1").fechaComprobante(COMPROBANTE)
                .lineas(List.of(new CuentaPorPagarLineaDTO("Mercadería", gasto.getIdCuenta(), "08", new BigDecimal("1000.00"),
                        new BigDecimal("130.00"), TratamientoIvaCompra.PLENO, null))).build());
        assertThat(saldo(proveedor)).isEqualByComparingTo("1130.00");

        cxpService.anular(cpp.getId(), "OC mal recibida", usuario);

        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getEstado()).isEqualTo(EstadoCuentaPorPagar.ANULADA);
            assertThat(g.getOrdenCompra().getId()).isEqualTo(oc.getId());
            assertThat(ordenCompraRepository.findById(oc.getId()).orElseThrow().getEstado())
                    .isEqualTo(EstadoOrdenCompra.RECIBIDA);
        });
        assertThat(saldo(proveedor)).isEqualByComparingTo("0.00");
    }

    // ── Saldo del proveedor en CRC ───────────────────────────────────────────

    @Test
    @DisplayName("saldo del proveedor en CRC: CxP de 113 000 CRC + CxP de 113 USD con TC 500 = 169 500,00")
    void saldoProveedor_monedasMezcladas() {
        cxpService.crear(dto("FE-CRC", "100000.00", "13000.00"));
        CuentaPorPagarDTO usd = dto("FE-USD", "100.00", "13.00");
        usd.setMoneda(MonedaFE.USD);
        usd.setTipoCambio(new BigDecimal("500"));
        cxpService.crear(usd);

        assertThat(saldo(proveedor)).isEqualByComparingTo("169500.00");
    }

    @Test
    @DisplayName("aplicar y revertir un pago recalculan el saldo en CRC (pago de 13 USD con TC 500 baja 6 500)")
    void saldoProveedor_aplicarYRevertirPago() {
        cxpService.crear(dto("FE-CRC", "100000.00", "13000.00"));
        CuentaPorPagarDTO usd = dto("FE-USD", "100.00", "13.00");
        usd.setMoneda(MonedaFE.USD);
        usd.setTipoCambio(new BigDecimal("500"));
        CuentaPorPagar cppUsd = cxpService.crear(usd);

        cxpService.aplicarPago(cppUsd, new BigDecimal("13.00"));
        assertThat(saldo(proveedor)).isEqualByComparingTo("163000.00");
        tx.executeWithoutResult(s -> cxpService.revertirPago(
                cxpRepository.findById(cppUsd.getId()).orElseThrow(), new BigDecimal("13.00")));
        assertThat(saldo(proveedor)).isEqualByComparingTo("169500.00");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BigDecimal saldo(Proveedor p) {
        return proveedorRepository.findById(p.getId()).orElseThrow().getSaldoPendiente();
    }

    private CuentaPorPagarDTO dto(String numero, String base, String iva) {
        BigDecimal b = new BigDecimal(base);
        BigDecimal i = new BigDecimal(iva);
        return CuentaPorPagarDTO.builder()
                .proveedorId(proveedor.getId())
                .descripcion("Mantenimiento de equipos")
                .numeroComprobanteProveedor(numero)
                .fechaComprobante(COMPROBANTE)
                .lineas(List.of(new CuentaPorPagarLineaDTO("Mantenimiento", gasto.getIdCuenta(), "08", b, i,
                        TratamientoIvaCompra.PLENO, null)))
                .moneda(MonedaFE.CRC)
                .build();
    }

    /** CxP guardada directamente (sin servicio ni asiento), como las heredadas o las de una prueba de índice. */
    private CuentaPorPagar cxpDirecta(String numero, String comprobante, String normalizado) {
        return CuentaPorPagar.builder().numero(numero).proveedor(proveedor).descripcion("Directa")
                .monto(new BigDecimal("100.00")).saldoPendiente(new BigDecimal("100.00")).moneda(MonedaFE.CRC)
                .tipoCambio(BigDecimal.ONE).fechaEmision(COMPROBANTE)
                .numeroComprobanteProveedor(comprobante).comprobanteNormalizado(normalizado).build();
    }

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

    private CuentaContable cuenta(String codigo, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setTipo(tipo);
        c.setNivel(4);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
