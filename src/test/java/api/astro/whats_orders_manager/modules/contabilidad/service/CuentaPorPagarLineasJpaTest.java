package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.config.ClockConfig;
import api.astro.whats_orders_manager.modules.contabilidad.dto.AceptacionComprobanteDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarVistaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagarLinea;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ProrrataIvaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.impl.CuentaPorPagarServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
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
import java.time.temporal.ChronoUnit;
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
 * T-054 (ADR-067 §2-4): CxP por líneas con cuenta y tratamiento de IVA contra una base real (H2, JPA): totales desde las
 * líneas, IVA acreditable por tratamiento, prorrata por año del comprobante (409 si falta), asiento por cuenta con el IVA
 * no acreditable sumado al costo, anulación que lo revierte entero y aceptación del comprobante (fechas, filtro, días).
 * Sin transacción envolvente: el servicio confirma o revierte la suya como en producción.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({AsientoContableService.class, CuentaPorPagarServiceImpl.class, TipoCambioService.class,
        ComprobanteProveedorValidator.class, api.astro.whats_orders_manager.modules.politicas.PoliticaIvaCompraGeneral.class, ClockConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-054 — CxP por líneas: tratamientos de IVA, prorrata, asiento por cuenta y aceptación (H2)")
class CuentaPorPagarLineasJpaTest {

    private static final LocalDate COMPROBANTE = LocalDate.of(2026, 9, 15);
    private int secuencia;

    @Autowired private CuentaPorPagarServiceImpl cxpService;
    @Autowired private CuentaPorPagarRepository cxpRepository;
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
    private Usuario usuario;
    private CuentaContable gasto;
    private CuentaContable mercaderia;
    private CuentaContable inventario;
    private CuentaContable ivaCredito;
    private CuentaContable cuentaPorPagar;
    private CuentaContable compras;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now(new ClockConfig().clock());
        usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(7);
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            gasto = cuenta("5.1.03.001", TipoCuenta.EGRESO);
            mercaderia = cuenta("5.3.01.001", TipoCuenta.EGRESO);
            inventario = cuenta("1.1.04.001", TipoCuenta.ACTIVO);
            ivaCredito = cuenta("1.1.03", TipoCuenta.ACTIVO);
            cuentaPorPagar = cuenta("2.1.05.001", TipoCuenta.PASIVO);
            compras = cuenta("5.1.01", TipoCuenta.EGRESO);
            param("CPP_CUENTA_COMPRAS", compras);
            param("COMPRAS_MERCADERIA", mercaderia);
            param("CPP_CUENTA_POR_PAGAR", cuentaPorPagar);
            param("CPP_IVA_CREDITO_FISCAL", ivaCredito);
            proveedor = proveedorRepository.save(Proveedor.builder().cedula("3101123456").nombre("Distribuidora S.A.")
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
            prorrataRepository.deleteAllInBatch();
            parametroRepository.deleteAllInBatch();
            cuentaRepository.deleteAllInBatch();
            proveedorRepository.deleteAllInBatch();
        });
    }

    // ── Totales y asiento por cuenta ─────────────────────────────────────────

    @Test
    @DisplayName("tres líneas en dos cuentas: totales desde las líneas; asiento con un débito por cuenta (agrupado), IVA y CxP; cuadra")
    void variasCuentas_agrupaPorCuenta() {
        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(gasto, "100000.00", "13000.00", TratamientoIvaCompra.PLENO, null),
                linea(mercaderia, "50000.00", "6500.00", TratamientoIvaCompra.PLENO, null),
                linea(gasto, "20000.00", "2600.00", TratamientoIvaCompra.PLENO, null)));

        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getSubtotal()).isEqualByComparingTo("170000.00");
            assertThat(g.getImpuesto()).isEqualByComparingTo("22100.00");
            assertThat(g.getMonto()).isEqualByComparingTo("192100.00");
            assertThat(g.getSaldoPendiente()).isEqualByComparingTo("192100.00");
            assertThat(g.getEstadoAceptacion()).isEqualTo(EstadoAceptacionComprobante.PENDIENTE);
            assertThat(g.getLineas()).extracting(CuentaPorPagarLinea::getNumeroLinea).containsExactly(1, 2, 3);
            assertThat(g.getLineas()).extracting(l -> l.getCuentaContable().getCodigo())
                    .containsExactly("5.1.03.001", "5.3.01.001", "5.1.03.001");
            assertThat(g.getLineas()).allSatisfy(l -> {
                assertThat(l.getTratamiento()).isEqualTo(TratamientoIvaCompra.PLENO);
                assertThat(l.getIvaAcreditable()).isEqualByComparingTo(l.getIva());
                assertThat(l.getIvaNoAcreditable()).isEqualByComparingTo("0.00");
            });
            List<AsientoContable> asientos = asientoRepository.findAll();
            assertThat(asientos).hasSize(1);
            AsientoContable a = asientos.get(0);
            assertThat(a.getFecha()).isEqualTo(COMPROBANTE);
            assertThat(a.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(a.estaCuadrado()).isTrue();
            assertThat(a.getDetalles()).as("un débito por cuenta, IVA y CxP").hasSize(4);
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(gasto.getIdCuenta())).isEqualByComparingTo("120000.00");
            assertThat(saldos.get(mercaderia.getIdCuenta())).isEqualByComparingTo("50000.00");
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("22100.00");
            assertThat(saldos.get(cuentaPorPagar.getIdCuenta())).isEqualByComparingTo("-192100.00");
            assertThat(saldos).doesNotContainKey(compras.getIdCuenta());
        });
    }

    @Test
    @DisplayName("IVA no acreditable: una compra al 13 % limitada al 1 % → acreditable 10,00, no acreditable 120,00 sumado a la cuenta de la línea")
    void limitado_trece_aUno() {
        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(mercaderia, "1000.00", "130.00", TratamientoIvaCompra.LIMITADO, "1")));

        tx.executeWithoutResult(s -> {
            CuentaPorPagarLinea l = cxpRepository.findById(cpp.getId()).orElseThrow().getLineas().get(0);
            assertThat(l.getTarifaLimite()).isEqualByComparingTo("1.00");
            assertThat(l.getIvaAcreditable()).isEqualByComparingTo("10.00");
            assertThat(l.getIvaNoAcreditable()).isEqualByComparingTo("120.00");
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(mercaderia.getIdCuenta())).as("base 1000 + no acreditable 120").isEqualByComparingTo("1120.00");
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("10.00");
            assertThat(saldos.get(cuentaPorPagar.getIdCuenta())).isEqualByComparingTo("-1130.00");
        });
    }

    @Test
    @DisplayName("NO_ACREDITABLE: todo el IVA va a la cuenta de la línea y no hay débito a IVA crédito fiscal")
    void noAcreditable_sinLineaDeIva() {
        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.NO_ACREDITABLE, null)));

        tx.executeWithoutResult(s -> {
            AsientoContable a = asientoRepository.findAll().get(0);
            assertThat(a.getDetalles()).hasSize(2);
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(gasto.getIdCuenta())).isEqualByComparingTo("1130.00");
            assertThat(saldos).doesNotContainKey(ivaCredito.getIdCuenta());
            assertThat(cxpRepository.findById(cpp.getId()).orElseThrow().getLineas().get(0).getIvaAcreditable())
                    .isEqualByComparingTo("0.00");
        });
    }

    @Test
    @DisplayName("PLENO, LIMITADO y NO_ACREDITABLE en una misma CxP: el asiento suma cada no acreditable a su cuenta y cuadra")
    void tratamientosMezclados() {
        cxpService.crear(dto(
                linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null),
                linea(mercaderia, "1000.00", "130.00", TratamientoIvaCompra.LIMITADO, "1"),
                linea(inventario, "200.00", "26.00", TratamientoIvaCompra.NO_ACREDITABLE, null)));

        tx.executeWithoutResult(s -> {
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(gasto.getIdCuenta())).isEqualByComparingTo("1000.00");
            assertThat(saldos.get(mercaderia.getIdCuenta())).isEqualByComparingTo("1120.00");
            assertThat(saldos.get(inventario.getIdCuenta())).isEqualByComparingTo("226.00");
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("140.00");
            assertThat(saldos.get(cuentaPorPagar.getIdCuenta())).isEqualByComparingTo("-2486.00");
            assertThat(asientoRepository.findAll().get(0).estaCuadrado()).isTrue();
        });
    }

    // ── Prorrata ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("USO_COMUN con prorrata del año del comprobante (80 %): acreditable 104,00, no acreditable 26,00 y el porcentaje queda en la línea")
    void usoComun_conProrrata() {
        guardarProrrata(2026, "80.00");

        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.USO_COMUN, null)));

        tx.executeWithoutResult(s -> {
            CuentaPorPagarLinea l = cxpRepository.findById(cpp.getId()).orElseThrow().getLineas().get(0);
            assertThat(l.getIvaAcreditable()).isEqualByComparingTo("104.00");
            assertThat(l.getIvaNoAcreditable()).isEqualByComparingTo("26.00");
            assertThat(l.getPorcentajeProrrata()).isEqualByComparingTo("80.00");
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(gasto.getIdCuenta())).isEqualByComparingTo("1026.00");
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("104.00");
        });
    }

    @Test
    @DisplayName("corregir el porcentaje del año no recalcula las CxP ya registradas")
    void correccionDeProrrata_noRecalculaLasExistentes() {
        guardarProrrata(2026, "80.00");
        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.USO_COMUN, null)));

        tx.executeWithoutResult(s -> {
            ProrrataIva p = prorrataRepository.findByAnio(2026).orElseThrow();
            p.setPorcentaje(new BigDecimal("50.00"));
            prorrataRepository.save(p);
        });

        tx.executeWithoutResult(s -> {
            CuentaPorPagarLinea l = cxpRepository.findById(cpp.getId()).orElseThrow().getLineas().get(0);
            assertThat(l.getIvaAcreditable()).isEqualByComparingTo("104.00");
            assertThat(l.getPorcentajeProrrata()).isEqualByComparingTo("80.00");
        });
    }

    @Test
    @DisplayName("USO_COMUN sin prorrata para el año del comprobante: 409 «No hay porcentaje de prorrata configurado para 2026» y no queda nada guardado")
    void usoComun_sinProrrata_409() {
        guardarProrrata(2025, "70.00"); // solo hay otro año

        assertThatThrownBy(() -> cxpService.crear(dto(
                linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.USO_COMUN, null))))
                .isInstanceOf(ProrrataNoConfiguradaException.class)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No hay porcentaje de prorrata configurado para 2026");

        assertThat(cxpRepository.count()).isZero();
        assertThat(asientoRepository.count()).isZero();
    }

    @Test
    @DisplayName("sin líneas de uso común no se exige prorrata")
    void sinUsoComun_noExigeProrrata() {
        assertThat(prorrataRepository.count()).isZero();
        assertThat(cxpService.crear(dto(linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null))).getId())
                .isNotNull();
    }

    // ── Validaciones ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("sin líneas, o con una línea inválida, se rechaza con el mensaje fijo y no queda nada guardado")
    void validaciones() {
        CuentaPorPagarDTO sinLineas = dto();
        assertThatThrownBy(() -> cxpService.crear(sinLineas)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ComprobanteProveedorValidator.MSG_LINEAS);
        assertThatThrownBy(() -> cxpService.crear(dto(linea(gasto, "0.00", "0.00", TratamientoIvaCompra.PLENO, null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(ComprobanteProveedorValidator.MSG_LINEA);
        assertThatThrownBy(() -> cxpService.crear(dto(linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.LIMITADO, "13"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(ComprobanteProveedorValidator.MSG_LIMITE);
        CuentaPorPagarDTO sinCuenta = dto(new CuentaPorPagarLineaDTO("x", null, "08", new BigDecimal("100"),
                new BigDecimal("13"), TratamientoIvaCompra.PLENO, null));
        assertThatThrownBy(() -> cxpService.crear(sinCuenta)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ComprobanteProveedorValidator.MSG_LINEA_CUENTA);
        assertThat(cxpRepository.count()).isZero();
        assertThat(asientoRepository.count()).isZero();
    }

    @Test
    @DisplayName("la cuenta de cada línea debe ser una hoja activa que acepta movimientos; una agrupadora o inactiva se rechaza")
    void cuentaDeLineaDebeSerHoja() {
        CuentaContable agrupadora = tx.execute(s -> {
            CuentaContable padre = cuenta("5.1.08", TipoCuenta.EGRESO);
            CuentaContable hija = cuenta("5.1.08.001", TipoCuenta.EGRESO);
            hija.setCuentaPadre(padre);
            cuentaRepository.save(hija);
            return padre;
        });
        CuentaContable inactiva = tx.execute(s -> {
            CuentaContable c = cuenta("5.1.09.001", TipoCuenta.EGRESO);
            c.setActiva(false);
            return cuentaRepository.save(c);
        });

        assertThatThrownBy(() -> cxpService.crear(dto(linea(agrupadora, "100.00", "13.00", TratamientoIvaCompra.PLENO, null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("subcuentas");
        assertThatThrownBy(() -> cxpService.crear(dto(
                linea(gasto, "100.00", "13.00", TratamientoIvaCompra.PLENO, null),
                linea(inactiva, "100.00", "13.00", TratamientoIvaCompra.PLENO, null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("inactiva");
        assertThat(cxpRepository.count()).isZero();
    }

    @Test
    @DisplayName("la cuenta de COMPRAS_MERCADERIA (5.3.01.001) y la de inventario sí son elegibles como cuenta de línea")
    void comprasMercaderiaEsElegible() {
        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(mercaderia, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null),
                linea(inventario, "500.00", "65.00", TratamientoIvaCompra.PLENO, null)));
        assertThat(cpp.getId()).isNotNull();
    }

    @Test
    @DisplayName("el IVA de una línea distinto de base × tarifa solo da aviso: la CxP se crea con el IVA enviado")
    void ivaDistintoDaAviso() {
        CuentaPorPagarDTO dto = dto(linea(gasto, "1000.00", "129.00", TratamientoIvaCompra.PLENO, null));
        assertThat(cxpService.advertencias(dto)).hasSize(1);
        CuentaPorPagar cpp = cxpService.crear(dto);
        assertThat(cxpService.findById(cpp.getId()).getMonto()).isEqualByComparingTo("1129.00");
    }

    @Test
    @DisplayName("el subtotal, impuesto y monto que envíe el cliente se ignoran: mandan las líneas")
    void totalesDelClienteSeIgnoran() {
        CuentaPorPagarDTO dto = dto(linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null));
        dto.setSubtotal(new BigDecimal("1.00"));
        dto.setImpuesto(new BigDecimal("2.00"));
        dto.setMonto(new BigDecimal("3.00"));
        CuentaPorPagar cpp = cxpService.crear(dto);
        assertThat(cpp.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(cpp.getImpuesto()).isEqualByComparingTo("130.00");
        assertThat(cpp.getMonto()).isEqualByComparingTo("1130.00");
    }

    @Test
    @DisplayName("se mantiene el bloqueo de duplicados de T-052 con líneas")
    void duplicadoSigueBloqueado() {
        CuentaPorPagarDTO primero = dto(linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null));
        primero.setNumeroComprobanteProveedor("FE-DUP");
        cxpService.crear(primero);
        CuentaPorPagarDTO repetido = dto(linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null));
        repetido.setNumeroComprobanteProveedor(" fe-dup ");
        assertThatThrownBy(() -> cxpService.crear(repetido)).isInstanceOf(ComprobanteProveedorDuplicadoException.class);
        assertThat(cxpRepository.count()).isEqualTo(1);
    }

    // ── Anulación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("anular una CxP con varias cuentas e IVA no acreditable revierte el asiento entero: todas las cuentas quedan en 0")
    void anular_revierteTodo() {
        guardarProrrata(2026, "80.00");
        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null),
                linea(mercaderia, "1000.00", "130.00", TratamientoIvaCompra.LIMITADO, "1"),
                linea(inventario, "200.00", "26.00", TratamientoIvaCompra.USO_COMUN, null),
                linea(compras, "300.00", "39.00", TratamientoIvaCompra.NO_ACREDITABLE, null)));

        cxpService.anular(cpp.getId(), "Registrada por error", usuario);

        tx.executeWithoutResult(s -> {
            assertThat(cxpRepository.findById(cpp.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCuentaPorPagar.ANULADA);
            List<AsientoContable> asientos = asientoRepository.findAll();
            assertThat(asientos).hasSize(2);
            AsientoContable reversion = asientos.stream().filter(a -> a.getAsientoRevertido() != null).findFirst().orElseThrow();
            assertThat(reversion.estaCuadrado()).isTrue();
            assertThat(reversion.getDetalles()).hasSize(6);
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos).hasSize(6);
            saldos.values().forEach(v -> assertThat(v).isEqualByComparingTo("0.00"));
        });
        assertThat(proveedorRepository.findById(proveedor.getId()).orElseThrow().getSaldoPendiente()).isEqualByComparingTo("0.00");
    }

    // ── Aceptación del comprobante ───────────────────────────────────────────

    @Test
    @DisplayName("la CxP nace con aceptación PENDIENTE, sin fecha ni número")
    void aceptacionPorDefecto() {
        CuentaPorPagar cpp = crearSimple("FE-A1");
        CuentaPorPagarVistaDTO v = cxpService.detalleVista(cpp.getId());
        assertThat(v.estadoAceptacion()).isEqualTo("PENDIENTE");
        assertThat(v.fechaAceptacion()).isNull();
        assertThat(v.numeroMensajeReceptor()).isNull();
    }

    @Test
    @DisplayName("registrar la aceptación guarda estado, fecha y número, y deja auditoría")
    void registrarAceptacion_ok() {
        CuentaPorPagar cpp = crearSimple("FE-A2");

        cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, COMPROBANTE.plusDays(2), " MR-0001 "), usuario);

        CuentaPorPagarVistaDTO v = cxpService.detalleVista(cpp.getId());
        assertThat(v.estadoAceptacion()).isEqualTo("ACEPTADO");
        assertThat(v.fechaAceptacion()).isEqualTo(COMPROBANTE.plusDays(2));
        assertThat(v.numeroMensajeReceptor()).isEqualTo("MR-0001");
        assertThat(v.diasDesdeComprobante()).as("solo las pendientes muestran los días").isNull();
        verify(actividadService).registrarActividad(eq(7), anyString(), contains("ACEPTADO"), eq("CuentaPorPagar"),
                eq(cpp.getId().intValue()));
    }

    @Test
    @DisplayName("la fecha de aceptación puede ser la del comprobante o hoy; RECHAZADO también se registra")
    void registrarAceptacion_limitesDeFecha() {
        CuentaPorPagar cpp = crearSimple("FE-A3");
        cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.RECHAZADO, COMPROBANTE, "MR-2"), usuario);
        cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, hoy, "MR-3"), usuario);
        assertThat(cxpService.detalleVista(cpp.getId()).estadoAceptacion()).isEqualTo("ACEPTADO");
    }

    @Test
    @DisplayName("fechas inválidas: futura o anterior a la del comprobante → 400 con mensaje fijo y sin cambios ni auditoría")
    void registrarAceptacion_fechasInvalidas() {
        CuentaPorPagar cpp = crearSimple("FE-A4");

        assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, hoy.plusDays(1), "MR-1"), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_FECHA_FUTURA);
        assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, COMPROBANTE.minusDays(1), "MR-1"), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_FECHA_ANTERIOR);
        assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, null, "MR-1"), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_FECHA);

        assertThat(cxpService.detalleVista(cpp.getId()).estadoAceptacion()).isEqualTo("PENDIENTE");
        verifyNoInteractions(actividadService);
    }

    @Test
    @DisplayName("estado distinto de ACEPTADO/RECHAZADO y número vacío o de más de 50 caracteres → 400 con mensaje fijo")
    void registrarAceptacion_estadoYNumero() {
        CuentaPorPagar cpp = crearSimple("FE-A5");
        for (EstadoAceptacionComprobante e : new EstadoAceptacionComprobante[] {null, EstadoAceptacionComprobante.PENDIENTE}) {
            assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                    new AceptacionComprobanteDTO(e, COMPROBANTE, "MR-1"), usuario))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_ESTADO);
        }
        for (String numero : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                    new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, COMPROBANTE, numero), usuario))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_NUMERO);
        }
        assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, COMPROBANTE, "N".repeat(51)), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_NUMERO_LARGO);
    }

    @Test
    @DisplayName("una CxP anulada no admite aceptación (409) y una inexistente da 404")
    void registrarAceptacion_anuladaOInexistente() {
        CuentaPorPagar cpp = crearSimple("FE-A6");
        cxpService.anular(cpp.getId(), "Error de captura", usuario);

        assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, COMPROBANTE, "MR-1"), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_ANULADA);
        assertThatThrownBy(() -> cxpService.registrarAceptacion(999999L,
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, COMPROBANTE, "MR-1"), usuario))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("el listado filtra por estado de aceptación (combinable con el estado) y las pendientes traen los días desde el comprobante")
    void listadoFiltraPorAceptacion() {
        CuentaPorPagar pendiente = crearSimple("FE-L1");
        CuentaPorPagar aceptada = crearSimple("FE-L2");
        CuentaPorPagar rechazada = crearSimple("FE-L3");
        cxpService.registrarAceptacion(aceptada.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, COMPROBANTE, "MR-1"), usuario);
        cxpService.registrarAceptacion(rechazada.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.RECHAZADO, COMPROBANTE, "MR-2"), usuario);

        assertThat(cxpService.listarVista(null, null)).hasSize(3);
        List<CuentaPorPagarVistaDTO> pendientes = cxpService.listarVista(null, EstadoAceptacionComprobante.PENDIENTE);
        assertThat(pendientes).extracting(CuentaPorPagarVistaDTO::id).containsExactly(pendiente.getId());
        assertThat(pendientes.get(0).diasDesdeComprobante()).isEqualTo(ChronoUnit.DAYS.between(COMPROBANTE, hoy));
        assertThat(cxpService.listarVista(null, EstadoAceptacionComprobante.ACEPTADO)).extracting(CuentaPorPagarVistaDTO::id)
                .containsExactly(aceptada.getId());
        assertThat(cxpService.listarVista(null, EstadoAceptacionComprobante.RECHAZADO)).extracting(CuentaPorPagarVistaDTO::id)
                .containsExactly(rechazada.getId());
        assertThat(cxpService.listarVista(EstadoCuentaPorPagar.PAGADA, EstadoAceptacionComprobante.PENDIENTE)).isEmpty();
        assertThat(cxpService.listarVista(EstadoCuentaPorPagar.PENDIENTE, EstadoAceptacionComprobante.ACEPTADO)).hasSize(1);
    }

    @Test
    @DisplayName("el detalle expone las líneas con código y nombre de cuenta, tarifa, tratamiento y montos")
    void detalleExponeLasLineas() {
        guardarProrrata(2026, "80.00");
        CuentaPorPagar cpp = cxpService.crear(dto(
                linea(mercaderia, "1000.00", "130.00", TratamientoIvaCompra.LIMITADO, "1"),
                linea(gasto, "100.00", "13.00", TratamientoIvaCompra.USO_COMUN, null)));

        CuentaPorPagarVistaDTO v = cxpService.detalleVista(cpp.getId());

        assertThat(v.lineas()).hasSize(2);
        CuentaPorPagarVistaDTO.Linea l1 = v.lineas().get(0);
        assertThat(l1.numeroLinea()).isEqualTo(1);
        assertThat(l1.cuentaCodigo()).isEqualTo("5.3.01.001");
        assertThat(l1.cuentaNombre()).isEqualTo("Cuenta 5.3.01.001");
        assertThat(l1.codigoTarifa()).isEqualTo("08");
        assertThat(l1.porcentaje()).isEqualByComparingTo("13");
        assertThat(l1.tratamiento()).isEqualTo("LIMITADO");
        assertThat(l1.tarifaLimite()).isEqualByComparingTo("1");
        assertThat(l1.ivaAcreditable()).isEqualByComparingTo("10.00");
        assertThat(l1.ivaNoAcreditable()).isEqualByComparingTo("120.00");
        assertThat(v.lineas().get(1).porcentajeProrrata()).isEqualByComparingTo("80.00");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CuentaPorPagar crearSimple(String comprobante) {
        CuentaPorPagarDTO dto = dto(linea(gasto, "1000.00", "130.00", TratamientoIvaCompra.PLENO, null));
        dto.setNumeroComprobanteProveedor(comprobante);
        return cxpService.crear(dto);
    }

    private void guardarProrrata(int anio, String porcentaje) {
        tx.executeWithoutResult(s -> prorrataRepository.save(ProrrataIva.builder().anio(anio)
                .porcentaje(new BigDecimal(porcentaje)).build()));
    }

    private CuentaPorPagarLineaDTO linea(CuentaContable cuenta, String base, String iva, TratamientoIvaCompra t, String limite) {
        return new CuentaPorPagarLineaDTO("Línea " + cuenta.getCodigo(), cuenta.getIdCuenta(), "08", new BigDecimal(base),
                new BigDecimal(iva), t, limite == null ? null : new BigDecimal(limite));
    }

    private CuentaPorPagarDTO dto(CuentaPorPagarLineaDTO... lineas) {
        return CuentaPorPagarDTO.builder()
                .proveedorId(proveedor.getId())
                .descripcion("Compra de prueba")
                .numeroComprobanteProveedor("FE-" + (++secuencia))
                .fechaComprobante(COMPROBANTE)
                .lineas(List.of(lineas))
                .moneda(MonedaFE.CRC)
                .build();
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
