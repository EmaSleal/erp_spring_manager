package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.config.ClockConfig;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
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
import api.astro.whats_orders_manager.modules.contabilidad.repository.ProrrataIvaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorDuplicadoException;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorValidator;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarCreada;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.contabilidad.service.OrdenCompraNoAdmiteFacturaException;
import api.astro.whats_orders_manager.modules.contabilidad.service.impl.CuentaPorPagarServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.proveedores.dto.OrdenCompraFacturacionDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.DetalleOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-053 (ADR-066 §4, ADR-067): las CxP de una orden de compra salen de cada factura del proveedor, por líneas con cuenta,
 * contra una base real (H2, JPA). Varias CxP por OC con facturado y pendiente, exceso con advertencia, duplicado, fecha del
 * comprobante en el asiento, cuenta propuesta (parámetro COMPRAS_MERCADERIA, opcional), la ruta vieja sin comprobante
 * eliminada y la anulación que recalcula lo facturado.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({AsientoContableService.class, CuentaPorPagarServiceImpl.class, OrdenCompraFacturacionService.class,
        TipoCambioService.class, ComprobanteProveedorValidator.class, ClockConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-053 — OC: CxP desde las facturas del proveedor, facturado y pendiente (H2)")
class OrdenCompraFacturacionJpaTest {

    private static final LocalDate COMPROBANTE = LocalDate.of(2026, 9, 15);
    private int secuencia;

    @Autowired private CuentaPorPagarServiceImpl cxpService;
    @Autowired private OrdenCompraFacturacionService facturacionService;
    @Autowired private CuentaPorPagarRepository cxpRepository;
    @Autowired private OrdenCompraRepository ocRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ProrrataIvaRepository prorrataRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private PagoProveedorRepository pagoRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private UsuarioActividadService actividadService;
    @MockitoBean private api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService topeComprasRts;

    private TransactionTemplate tx;
    private Proveedor proveedor;
    private Proveedor otroProveedor;
    private Usuario usuario;
    private CuentaContable mercaderia;
    private CuentaContable flete;
    private CuentaContable ivaCredito;
    private CuentaContable cuentaPorPagar;
    private CuentaContable compras;
    private OrdenCompra oc;

    @BeforeEach
    void setUp() {
        usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(7);
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            mercaderia = cuenta("5.3.01.001", TipoCuenta.EGRESO);
            flete = cuenta("5.3.01.002", TipoCuenta.EGRESO);
            ivaCredito = cuenta("1.1.03", TipoCuenta.ACTIVO);
            cuentaPorPagar = cuenta("2.1.05.001", TipoCuenta.PASIVO);
            compras = cuenta("5.1.01", TipoCuenta.EGRESO);
            param("CPP_CUENTA_COMPRAS", compras);
            param("COMPRAS_MERCADERIA", mercaderia);
            param("CPP_CUENTA_POR_PAGAR", cuentaPorPagar);
            param("CPP_IVA_CREDITO_FISCAL", ivaCredito);
            proveedor = proveedorRepository.save(Proveedor.builder().cedula("3101123456").nombre("Distribuidora S.A.")
                    .tipoProveedor(TipoProveedor.values()[0]).build());
            otroProveedor = proveedorRepository.save(Proveedor.builder().cedula("3101999999").nombre("Otro S.A.")
                    .tipoProveedor(TipoProveedor.values()[0]).build());
            oc = guardarOc(EstadoOrdenCompra.RECIBIDA_PARCIAL, MonedaFE.CRC);
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
            ocRepository.deleteAll();
            ocRepository.flush();
            prorrataRepository.deleteAllInBatch();
            parametroRepository.deleteAllInBatch();
            cuentaRepository.deleteAllInBatch();
            proveedorRepository.deleteAllInBatch();
        });
    }

    // ── Varias CxP por OC: facturado y pendiente ─────────────────────────────

    @Test
    @DisplayName("dos facturas (entregas parciales): la OC muestra el total, lo facturado acumulado y el pendiente")
    void dosFacturas_facturadoYPendiente() {
        OrdenCompraFacturacionDTO antes = facturacionService.detalle(oc.getId());
        assertThat(antes.total()).isEqualByComparingTo("113000.00");
        assertThat(antes.facturado()).isEqualByComparingTo("0.00");
        assertThat(antes.pendiente()).isEqualByComparingTo("113000.00");
        assertThat(antes.cuentas()).isEmpty();

        CuentaPorPagarCreada primera = cxpService.crearConAdvertencias(dto("FE-1", linea(mercaderia, "50000.00", "6500.00")));
        assertThat(primera.advertencias()).isEmpty();
        OrdenCompraFacturacionDTO a = facturacionService.detalle(oc.getId());
        assertThat(a.facturado()).isEqualByComparingTo("56500.00");
        assertThat(a.pendiente()).isEqualByComparingTo("56500.00");
        assertThat(a.exceso()).isEqualByComparingTo("0.00");

        CuentaPorPagarCreada segunda = cxpService.crearConAdvertencias(dto("FE-2", linea(mercaderia, "50000.00", "6500.00")));
        assertThat(segunda.advertencias()).isEmpty();
        OrdenCompraFacturacionDTO d = facturacionService.detalle(oc.getId());
        assertThat(d.facturado()).isEqualByComparingTo("113000.00");
        assertThat(d.pendiente()).isEqualByComparingTo("0.00");
        assertThat(d.cuentas()).hasSize(2);
        assertThat(d.cuentas()).extracting(OrdenCompraFacturacionDTO.Cuenta::numeroComprobanteProveedor)
                .containsExactlyInAnyOrder("FE-1", "FE-2");
        assertThat(d.cuentas()).allSatisfy(c -> {
            assertThat(c.fechaComprobante()).isEqualTo(COMPROBANTE);
            assertThat(c.estadoAceptacion()).isEqualTo("PENDIENTE");
            assertThat(c.estado()).isEqualTo("PENDIENTE");
        });
        assertThat(d.detalles()).hasSize(2);
    }

    @Test
    @DisplayName("lo facturado supera el total de la OC: la CxP se registra y la respuesta trae la advertencia con la diferencia")
    void exceso_advierteSinBloquear() {
        cxpService.crearConAdvertencias(dto("FE-1", linea(mercaderia, "100000.00", "13000.00")));
        CuentaPorPagarCreada exceso = cxpService.crearConAdvertencias(dto("FE-2", linea(mercaderia, "10000.00", "1300.00")));

        assertThat(exceso.cuenta().getId()).isNotNull();
        assertThat(exceso.advertencias()).containsExactly("Lo facturado (124300.00) supera el total de la orden de compra "
                + oc.getNumero() + " (113000.00) por 11300.00");
        OrdenCompraFacturacionDTO d = facturacionService.detalle(oc.getId());
        assertThat(d.facturado()).isEqualByComparingTo("124300.00");
        assertThat(d.pendiente()).isEqualByComparingTo("0.00");
        assertThat(d.exceso()).isEqualByComparingTo("11300.00");
    }

    @Test
    @DisplayName("anular una CxP de la OC recalcula lo facturado y conserva el enlace con la OC")
    void anular_recalculaFacturado() {
        CuentaPorPagar uno = cxpService.crear(dto("FE-1", linea(mercaderia, "50000.00", "6500.00")));
        CuentaPorPagar dos = cxpService.crear(dto("FE-2", linea(mercaderia, "20000.00", "2600.00")));
        assertThat(facturacionService.detalle(oc.getId()).facturado()).isEqualByComparingTo("79100.00");

        cxpService.anular(dos.getId(), "Factura duplicada", usuario);

        OrdenCompraFacturacionDTO d = facturacionService.detalle(oc.getId());
        assertThat(d.facturado()).isEqualByComparingTo("56500.00");
        assertThat(d.pendiente()).isEqualByComparingTo("56500.00");
        assertThat(d.cuentas()).hasSize(2);
        assertThat(d.cuentas()).filteredOn(c -> c.id().equals(dos.getId())).extracting(OrdenCompraFacturacionDTO.Cuenta::estado)
                .containsExactly("ANULADA");
        tx.executeWithoutResult(s -> assertThat(cxpRepository.findById(dos.getId()).orElseThrow().getOrdenCompra().getId())
                .isEqualTo(oc.getId()));
        // y el comprobante anulado se puede volver a registrar
        assertThat(cxpService.crear(dto("FE-2", linea(mercaderia, "20000.00", "2600.00"))).getId()).isNotNull();
    }

    // ── Comprobante, duplicados y fecha ──────────────────────────────────────

    @Test
    @DisplayName("el mismo comprobante del mismo proveedor en la misma OC (o en una CxP manual) se rechaza como duplicado")
    void duplicado() {
        cxpService.crear(dto("FE-DUP", linea(mercaderia, "1000.00", "130.00")));
        assertThatThrownBy(() -> cxpService.crear(dto("FE-DUP", linea(mercaderia, "500.00", "65.00"))))
                .isInstanceOf(ComprobanteProveedorDuplicadoException.class);
        assertThatThrownBy(() -> cxpService.crear(dtoManual("FE-dup", linea(mercaderia, "500.00", "65.00"))))
                .isInstanceOf(ComprobanteProveedorDuplicadoException.class);
        assertThat(cxpRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("la fecha de la CxP y de su asiento es la del comprobante; los montos salen de la factura, no del total de la OC")
    void fechaDelComprobanteYMontosDeLaFactura() {
        CuentaPorPagar cpp = cxpService.crear(dto("FE-F", linea(mercaderia, "1000.00", "130.00")));
        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getFechaEmision()).isEqualTo(COMPROBANTE);
            assertThat(g.getFechaComprobante()).isEqualTo(COMPROBANTE);
            assertThat(g.getMonto()).isEqualByComparingTo("1130.00");
            assertThat(g.getSubtotal()).isEqualByComparingTo("1000.00");
            assertThat(g.getImpuesto()).isEqualByComparingTo("130.00");
            assertThat(g.getEstadoAceptacion()).isEqualTo(EstadoAceptacionComprobante.PENDIENTE);
            assertThat(g.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
            assertThat(g.getOrdenCompra().getId()).isEqualTo(oc.getId());
            List<AsientoContable> asientos = asientoRepository.findAll();
            assertThat(asientos).hasSize(1);
            assertThat(asientos.get(0).getFecha()).isEqualTo(COMPROBANTE);
            assertThat(asientos.get(0).estaCuadrado()).isTrue();
        });
    }

    @Test
    @DisplayName("el asiento debita la cuenta de cada línea (mercadería y flete), no la cuenta única de compras de la OC")
    void asientoPorLineas() {
        cxpService.crear(dto("FE-L", linea(mercaderia, "10000.00", "1300.00"), linea(flete, "2000.00", "260.00")));
        tx.executeWithoutResult(s -> {
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(mercaderia.getIdCuenta())).isEqualByComparingTo("10000.00");
            assertThat(saldos.get(flete.getIdCuenta())).isEqualByComparingTo("2000.00");
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("1560.00");
            assertThat(saldos.get(cuentaPorPagar.getIdCuenta())).isEqualByComparingTo("-13560.00");
            assertThat(saldos).doesNotContainKey(compras.getIdCuenta());
        });
    }

    @Test
    @DisplayName("ninguna ruta crea una CxP sin comprobante: con OC y sin número, sin fecha o sin líneas se rechaza con 400")
    void sinComprobante_seRechaza() {
        CuentaPorPagarDTO sinNumero = dto("", linea(mercaderia, "100.00", "13.00"));
        assertThatThrownBy(() -> cxpService.crear(sinNumero)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ComprobanteProveedorValidator.MSG_NUMERO);
        CuentaPorPagarDTO sinFecha = dto("FE-X", linea(mercaderia, "100.00", "13.00"));
        sinFecha.setFechaComprobante(null);
        assertThatThrownBy(() -> cxpService.crear(sinFecha)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ComprobanteProveedorValidator.MSG_FECHA);
        CuentaPorPagarDTO sinLineas = dto("FE-Y");
        assertThatThrownBy(() -> cxpService.crear(sinLineas)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ComprobanteProveedorValidator.MSG_LINEAS);
        assertThat(cxpRepository.count()).isZero();
    }

    @Test
    @DisplayName("la ruta vieja (crearDesdeOrdenCompra y POST /{id}/generar-cpp) ya no existe")
    void rutaViejaEliminada() {
        assertThat(CuentaPorPagarService.class.getMethods()).extracting(java.lang.reflect.Method::getName)
                .doesNotContain("crearDesdeOrdenCompra");
        assertThat(CuentaPorPagarServiceImpl.class.getDeclaredMethods()).extracting(java.lang.reflect.Method::getName)
                .doesNotContain("crearDesdeOrdenCompra");
        for (var m : api.astro.whats_orders_manager.modules.proveedores.controller.OrdenCompraRestController.class.getDeclaredMethods()) {
            var post = m.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
            if (post != null) {
                assertThat(post.value()).doesNotContain("/{id}/generar-cpp");
            }
        }
    }

    // ── Estado, proveedor y moneda de la OC ──────────────────────────────────

    @Test
    @DisplayName("la OC debe estar recibida (total o parcial): en otro estado, 409 con mensaje fijo y no se guarda nada")
    void estadoNoAdmiteFacturar() {
        for (EstadoOrdenCompra estado : List.of(EstadoOrdenCompra.BORRADOR, EstadoOrdenCompra.APROBADA, EstadoOrdenCompra.CANCELADA)) {
            tx.executeWithoutResult(s -> {
                OrdenCompra o = ocRepository.findById(oc.getId()).orElseThrow();
                o.setEstado(estado);
                ocRepository.save(o);
            });
            assertThatThrownBy(() -> cxpService.crear(dto("FE-E", linea(mercaderia, "100.00", "13.00"))))
                    .isInstanceOf(OrdenCompraNoAdmiteFacturaException.class)
                    .hasMessage("La orden de compra no admite facturas en estado " + estado
                            + "; debe estar recibida total o parcialmente");
        }
        assertThat(cxpRepository.count()).isZero();
        tx.executeWithoutResult(s -> {
            OrdenCompra o = ocRepository.findById(oc.getId()).orElseThrow();
            o.setEstado(EstadoOrdenCompra.RECIBIDA);
            ocRepository.save(o);
        });
        assertThat(cxpService.crear(dto("FE-E", linea(mercaderia, "100.00", "13.00"))).getId()).isNotNull();
        assertThat(facturacionService.detalle(oc.getId()).admiteFacturar()).isTrue();
    }

    @Test
    @DisplayName("el proveedor es el de la OC: se puede omitir y, si se envía otro, 400")
    void proveedorDeLaOc() {
        CuentaPorPagarDTO sinProveedor = dto("FE-P1", linea(mercaderia, "100.00", "13.00"));
        sinProveedor.setProveedorId(null);
        CuentaPorPagar cpp = cxpService.crear(sinProveedor);
        tx.executeWithoutResult(s -> assertThat(cxpRepository.findById(cpp.getId()).orElseThrow().getProveedor().getId())
                .isEqualTo(proveedor.getId()));

        CuentaPorPagarDTO otro = dto("FE-P2", linea(mercaderia, "100.00", "13.00"));
        otro.setProveedorId(otroProveedor.getId());
        assertThatThrownBy(() -> cxpService.crear(otro)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El proveedor de la cuenta por pagar debe ser el de la orden de compra");
    }

    @Test
    @DisplayName("la moneda es la de la OC: se hereda si se omite y, si difiere, 400")
    void monedaDeLaOc() {
        tx.executeWithoutResult(s -> {
            OrdenCompra o = ocRepository.findById(oc.getId()).orElseThrow();
            o.setMoneda(MonedaFE.USD);
            o.setTipoCambio(new BigDecimal("510.00000"));
            ocRepository.save(o);
        });
        CuentaPorPagarDTO crc = dto("FE-M1", linea(mercaderia, "100.00", "13.00"));
        assertThatThrownBy(() -> cxpService.crear(crc)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide con la moneda de la OC");
        CuentaPorPagarDTO heredada = dto("FE-M2", linea(mercaderia, "100.00", "13.00"));
        heredada.setMoneda(null);
        CuentaPorPagar cpp = cxpService.crear(heredada);
        assertThat(cpp.getMoneda()).isEqualTo(MonedaFE.USD);
        assertThat(cpp.getTipoCambio()).isEqualByComparingTo("510.00000");
    }

    // ── Cuenta propuesta (ADR-067, enmienda) ─────────────────────────────────

    @Test
    @DisplayName("con el parámetro COMPRAS_MERCADERIA configurado, el detalle de la OC propone su cuenta; sin él no propone ninguna y se puede registrar igual (sin 409)")
    void cuentaPropuesta() {
        OrdenCompraFacturacionDTO con = facturacionService.detalle(oc.getId());
        assertThat(con.cuentaPropuestaId()).isEqualTo(mercaderia.getIdCuenta());
        assertThat(con.cuentaPropuestaCodigo()).isEqualTo("5.3.01.001");

        tx.executeWithoutResult(s -> parametroRepository.delete(parametroRepository.findByClave("COMPRAS_MERCADERIA").orElseThrow()));
        OrdenCompraFacturacionDTO sin = facturacionService.detalle(oc.getId());
        assertThat(sin.cuentaPropuestaId()).isNull();
        assertThat(sin.cuentaPropuestaCodigo()).isNull();
        assertThat(sin.admiteFacturar()).isTrue();
        assertThat(cxpService.crear(dto("FE-SIN", linea(flete, "100.00", "13.00"))).getId()).isNotNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OrdenCompra guardarOc(EstadoOrdenCompra estado, MonedaFE moneda) {
        OrdenCompra o = OrdenCompra.builder().numero("OC-2026-00001").proveedor(proveedor).estado(estado)
                .fecha(LocalDate.of(2026, 9, 1)).moneda(moneda).tipoCambio(BigDecimal.ONE)
                .impuesto(new BigDecimal("13000.00")).build();
        o.getDetalles().add(DetalleOrdenCompra.builder().ordenCompra(o).descripcion("Resina A").cantidad(100)
                .precioUnitario(new BigDecimal("500.00")).total(new BigDecimal("50000.00")).build());
        o.getDetalles().add(DetalleOrdenCompra.builder().ordenCompra(o).descripcion("Resina B").cantidad(100)
                .precioUnitario(new BigDecimal("500.00")).total(new BigDecimal("50000.00")).build());
        o.recalcularTotales();
        return ocRepository.save(o);
    }

    private CuentaPorPagarLineaDTO linea(CuentaContable cuenta, String base, String iva) {
        return new CuentaPorPagarLineaDTO("Línea " + cuenta.getCodigo(), cuenta.getIdCuenta(), "08", new BigDecimal(base),
                new BigDecimal(iva), TratamientoIvaCompra.PLENO, null);
    }

    private CuentaPorPagarDTO dto(String comprobante, CuentaPorPagarLineaDTO... lineas) {
        return CuentaPorPagarDTO.builder().proveedorId(proveedor.getId()).ordenCompraId(oc.getId())
                .descripcion("Factura de la OC").numeroComprobanteProveedor(comprobante).fechaComprobante(COMPROBANTE)
                .lineas(List.of(lineas)).moneda(MonedaFE.CRC).build();
    }

    private CuentaPorPagarDTO dtoManual(String comprobante, CuentaPorPagarLineaDTO... lineas) {
        CuentaPorPagarDTO d = dto(comprobante, lineas);
        d.setOrdenCompraId(null);
        return d;
    }

    private Map<Long, BigDecimal> saldosPorCuenta() {
        Map<Long, BigDecimal> saldos = new HashMap<>();
        for (DetalleAsiento d : detalleRepository.findAll()) {
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
