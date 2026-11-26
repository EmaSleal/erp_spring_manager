package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.AceptacionComprobanteDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarVistaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
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
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.politicas.PoliticaIvaCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-058 criterios 1, 5, 6, 7 y 8 (ADR-074): la CxP según el régimen de la empresa en la fecha del comprobante (GENERAL hasta
 * 2026-12-31, SIMPLIFICADO desde 2027-01-01) y el régimen del proveedor. Hoy es 2027-03-01 (Clock fijo). Sin transacción
 * envolvente: el servicio confirma o revierte la suya como en producción.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({AsientoContableService.class, CuentaPorPagarServiceImpl.class, TipoCambioService.class,
        ComprobanteProveedorValidator.class, PoliticaIvaCompra.class, RegimenTributarioService.class,
        ConsultaDocumentosPorFecha.class, CuentaPorPagarRegimenJpaTest.RelojFijo.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-058 — CxP según el régimen de la empresa y del proveedor (H2)")
class CuentaPorPagarRegimenJpaTest {

    static final LocalDate FECHA_GENERAL = LocalDate.of(2026, 9, 15);
    static final LocalDate FECHA_SIMPLIFICADO = LocalDate.of(2027, 2, 1);
    static final LocalDate HOY = LocalDate.of(2027, 3, 1);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock clock() {
            return Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.of("America/Costa_Rica")).toInstant(),
                    ZoneId.of("America/Costa_Rica"));
        }
    }

    private int secuencia;

    @Autowired private CuentaPorPagarServiceImpl cxpService;
    @Autowired private CuentaPorPagarRepository cxpRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private PagoProveedorRepository pagoRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RegimenTributarioService regimenService;
    @MockitoBean private UsuarioActividadService actividadService;
    @MockitoBean private api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService topeComprasRts;

    private TransactionTemplate tx;
    private Usuario usuario;
    private CuentaContable mercaderia;
    private CuentaContable ivaCredito;
    private CuentaContable cuentaPorPagar;
    private Long proveedorGeneralId;
    private Long proveedorSimplificadoId;

    @BeforeEach
    void setUp() {
        usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(7);
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            mercaderia = cuenta("5.3.01.001", TipoCuenta.EGRESO);
            ivaCredito = cuenta("1.1.03", TipoCuenta.ACTIVO);
            cuentaPorPagar = cuenta("2.1.05.001", TipoCuenta.PASIVO);
            CuentaContable compras = cuenta("5.1.01", TipoCuenta.EGRESO);
            param("CPP_CUENTA_COMPRAS", compras);
            param("COMPRAS_MERCADERIA", mercaderia);
            param("CPP_CUENTA_POR_PAGAR", cuentaPorPagar);
            param("CPP_IVA_CREDITO_FISCAL", ivaCredito);
            proveedorGeneralId = proveedorRepository.save(Proveedor.builder().cedula("3101123456").nombre("Distribuidora S.A.")
                    .tipoProveedor(TipoProveedor.values()[0]).regimenHacienda(RegimenHacienda.GENERAL).build()).getId();
            proveedorSimplificadoId = proveedorRepository.save(Proveedor.builder().cedula("3101999999").nombre("Monrachem")
                    .tipoProveedor(TipoProveedor.values()[0]).regimenHacienda(RegimenHacienda.SIMPLIFICADO).build()).getId();
        });
        jdbc.update("INSERT INTO regimen_tributario (regimen, vigente_desde, nota, fecha_registro) VALUES "
                + "('GENERAL', '2000-01-01', 'prueba T-058', CURRENT_TIMESTAMP), "
                + "('SIMPLIFICADO', '2027-01-01', 'prueba T-058', CURRENT_TIMESTAMP)");
        regimenService.invalidarCache();
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
            parametroRepository.deleteAllInBatch();
            cuentaRepository.deleteAllInBatch();
            proveedorRepository.deleteAllInBatch();
        });
        jdbc.update("DELETE FROM regimen_tributario");
        regimenService.invalidarCache();
    }

    // ── Criterio 7: proveedor SIMPLIFICADO, tarifa del bien 13 %, IVA 0 ──────

    @Test
    @DisplayName("proveedor SIMPLIFICADO, empresa GENERAL: tarifa 08 conservada, IVA 0, NO_ACREDITABLE, asiento solo a la cuenta de compra")
    void proveedorSimplificado_empresaGeneral() {
        CuentaPorPagar cpp = cxpService.crear(dto(proveedorSimplificadoId, FECHA_GENERAL,
                linea("1000.00", "0.00", TratamientoIvaCompra.PLENO)));

        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getRegimenProveedor()).isEqualTo(RegimenHacienda.SIMPLIFICADO);
            assertThat(g.getEstadoAceptacion()).isEqualTo(EstadoAceptacionComprobante.PENDIENTE);
            CuentaPorPagarLinea l = g.getLineas().get(0);
            assertThat(l.getCodigoTarifa()).as("tarifa del bien").isEqualTo("08");
            assertThat(l.getIva()).as("IVA trasladado").isEqualByComparingTo("0.00");
            assertThat(l.getTratamiento()).isEqualTo(TratamientoIvaCompra.NO_ACREDITABLE);
            assertThat(l.getIvaAcreditable()).isEqualByComparingTo("0.00");
            assertThat(l.getIvaNoAcreditable()).isEqualByComparingTo("0.00");
            soloCuentaDeCompra("1000.00");
        });
    }

    @Test
    @DisplayName("proveedor SIMPLIFICADO, empresa SIMPLIFICADO: crédito 0, asiento solo a la cuenta de compra y aceptación NO_APLICA")
    void proveedorSimplificado_empresaSimplificado() {
        CuentaPorPagar cpp = cxpService.crear(dto(proveedorSimplificadoId, FECHA_SIMPLIFICADO,
                linea("1000.00", "0.00", TratamientoIvaCompra.NO_ACREDITABLE)));

        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getEstadoAceptacion()).isEqualTo(EstadoAceptacionComprobante.NO_APLICA);
            assertThat(g.getLineas().get(0).getIvaAcreditable()).isEqualByComparingTo("0.00");
            assertThat(g.getLineas().get(0).getCodigoTarifa()).isEqualTo("08");
            soloCuentaDeCompra("1000.00");
        });
    }

    @Test
    @DisplayName("proveedor SIMPLIFICADO con IVA trasladado > 0 → IllegalArgumentException (400) y nada guardado")
    void proveedorSimplificado_ivaMayorACero() {
        assertThatThrownBy(() -> cxpService.crear(dto(proveedorSimplificadoId, FECHA_GENERAL,
                linea("1000.00", "130.00", TratamientoIvaCompra.NO_ACREDITABLE))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(PoliticaIvaCompra.MSG_PROVEEDOR_SIN_IVA);
        assertThat(cxpRepository.count()).isZero();
    }

    @Test
    @DisplayName("proveedor GENERAL, empresa GENERAL: PLENO, USO_COMUN (409 sin prorrata) y NO_ACREDITABLE como en T-054")
    void proveedorGeneral_empresaGeneral() {
        CuentaPorPagar cpp = cxpService.crear(dto(proveedorGeneralId, FECHA_GENERAL,
                linea("1000.00", "130.00", TratamientoIvaCompra.PLENO)));
        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getRegimenProveedor()).isEqualTo(RegimenHacienda.GENERAL);
            assertThat(g.getEstadoAceptacion()).isEqualTo(EstadoAceptacionComprobante.PENDIENTE);
            assertThat(g.getLineas().get(0).getIvaAcreditable()).isEqualByComparingTo("130.00");
            Map<Long, BigDecimal> saldos = saldosPorCuenta();
            assertThat(saldos.get(ivaCredito.getIdCuenta())).isEqualByComparingTo("130.00");
            assertThat(saldos.get(mercaderia.getIdCuenta())).isEqualByComparingTo("1000.00");
        });
        assertThatThrownBy(() -> cxpService.crear(dto(proveedorGeneralId, FECHA_GENERAL,
                linea("1000.00", "130.00", TratamientoIvaCompra.USO_COMUN))))
                .isInstanceOf(ProrrataNoConfiguradaException.class);
        CuentaPorPagar noAcred = cxpService.crear(dto(proveedorGeneralId, FECHA_GENERAL,
                linea("1000.00", "130.00", TratamientoIvaCompra.NO_ACREDITABLE)));
        tx.executeWithoutResult(s -> {
            CuentaPorPagarLinea l = cxpRepository.findById(noAcred.getId()).orElseThrow().getLineas().get(0);
            assertThat(l.getIvaNoAcreditable()).isEqualByComparingTo("130.00");
        });
    }

    @Test
    @DisplayName("proveedor GENERAL, empresa SIMPLIFICADO: solo NO_ACREDITABLE; el IVA va a la cuenta de la línea; NO_APLICA")
    void proveedorGeneral_empresaSimplificado() {
        assertThatThrownBy(() -> cxpService.crear(dto(proveedorGeneralId, FECHA_SIMPLIFICADO,
                linea("1000.00", "130.00", TratamientoIvaCompra.PLENO))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(PoliticaIvaCompra.MSG_SOLO_NO_ACREDITABLE);
        assertThat(cxpRepository.count()).isZero();

        CuentaPorPagar cpp = cxpService.crear(dto(proveedorGeneralId, FECHA_SIMPLIFICADO,
                linea("1000.00", "130.00", TratamientoIvaCompra.NO_ACREDITABLE)));
        tx.executeWithoutResult(s -> {
            CuentaPorPagar g = cxpRepository.findById(cpp.getId()).orElseThrow();
            assertThat(g.getEstadoAceptacion()).isEqualTo(EstadoAceptacionComprobante.NO_APLICA);
            soloCuentaDeCompra("1130.00");
        });
    }

    @Test
    @DisplayName("fecha sin tramo → RegimenNoDefinidoException y nada guardado")
    void sinTramo() {
        assertThatThrownBy(() -> cxpService.crear(dto(proveedorGeneralId, LocalDate.of(1999, 12, 31),
                linea("1000.00", "130.00", TratamientoIvaCompra.PLENO))))
                .isInstanceOf(RegimenNoDefinidoException.class);
        assertThat(cxpRepository.count()).isZero();
    }

    // ── Criterio 5: instantánea del régimen del proveedor ────────────────────

    @Test
    @DisplayName("regimen_proveedor copia el del proveedor al registrar y no cambia si el proveedor se vuelve a verificar")
    void instantaneaDelRegimenDelProveedor() {
        CuentaPorPagar cpp = cxpService.crear(dto(proveedorSimplificadoId, FECHA_GENERAL,
                linea("1000.00", "0.00", TratamientoIvaCompra.NO_ACREDITABLE)));
        tx.executeWithoutResult(s -> {
            Proveedor p = proveedorRepository.findById(proveedorSimplificadoId).orElseThrow();
            p.setRegimenHacienda(RegimenHacienda.GENERAL);
            proveedorRepository.save(p);
        });
        tx.executeWithoutResult(s -> assertThat(cxpRepository.findById(cpp.getId()).orElseThrow().getRegimenProveedor())
                .isEqualTo(RegimenHacienda.SIMPLIFICADO));
    }

    // ── Criterio 8: NO_APLICA ────────────────────────────────────────────────

    @Test
    @DisplayName("NO_APLICA no pasa a ACEPTADO ni RECHAZADO (IllegalStateException → 409), no cuenta como pendiente y no tiene días")
    void noAplica_noSePuedeAceptarNiRechazar() {
        CuentaPorPagar cpp = cxpService.crear(dto(proveedorGeneralId, FECHA_SIMPLIFICADO,
                linea("1000.00", "130.00", TratamientoIvaCompra.NO_ACREDITABLE)));
        for (EstadoAceptacionComprobante e : List.of(EstadoAceptacionComprobante.ACEPTADO, EstadoAceptacionComprobante.RECHAZADO)) {
            assertThatThrownBy(() -> cxpService.registrarAceptacion(cpp.getId(),
                    new AceptacionComprobanteDTO(e, FECHA_SIMPLIFICADO, "MR-1"), usuario))
                    .isInstanceOf(IllegalStateException.class).hasMessage(CuentaPorPagarService.MSG_ACEPTACION_NO_APLICA);
        }
        CuentaPorPagarVistaDTO v = cxpService.detalleVista(cpp.getId());
        assertThat(v.estadoAceptacion()).isEqualTo("NO_APLICA");
        assertThat(v.diasDesdeComprobante()).isNull();
        assertThat(cxpService.listarVista(null, EstadoAceptacionComprobante.PENDIENTE)).isEmpty();
    }

    @Test
    @DisplayName("GENERAL sigue PENDIENTE y se puede aceptar")
    void general_sigueComoHoy() {
        CuentaPorPagar cpp = cxpService.crear(dto(proveedorGeneralId, FECHA_GENERAL,
                linea("1000.00", "130.00", TratamientoIvaCompra.PLENO)));
        assertThat(cxpService.detalleVista(cpp.getId()).estadoAceptacion()).isEqualTo("PENDIENTE");
        cxpService.registrarAceptacion(cpp.getId(),
                new AceptacionComprobanteDTO(EstadoAceptacionComprobante.ACEPTADO, FECHA_GENERAL.plusDays(1), "MR-2"), usuario);
        assertThat(cxpService.detalleVista(cpp.getId()).estadoAceptacion()).isEqualTo("ACEPTADO");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Un débito a la cuenta de compra y un crédito a la CxP por el total; ninguna línea de IVA acreditable. */
    private void soloCuentaDeCompra(String total) {
        Map<Long, BigDecimal> saldos = saldosPorCuenta();
        assertThat(saldos.get(mercaderia.getIdCuenta())).isEqualByComparingTo(total);
        assertThat(saldos.get(cuentaPorPagar.getIdCuenta())).isEqualByComparingTo("-" + total);
        assertThat(saldos).doesNotContainKey(ivaCredito.getIdCuenta());
        assertThat(detalleRepository.findAll()).hasSize(2);
    }

    private CuentaPorPagarLineaDTO linea(String base, String iva, TratamientoIvaCompra t) {
        return new CuentaPorPagarLineaDTO("Mercadería", mercaderia.getIdCuenta(), "08", new BigDecimal(base),
                new BigDecimal(iva), t, null);
    }

    private CuentaPorPagarDTO dto(Long proveedorId, LocalDate fecha, CuentaPorPagarLineaDTO... lineas) {
        return CuentaPorPagarDTO.builder().proveedorId(proveedorId).descripcion("Compra de prueba")
                .numeroComprobanteProveedor("FE-" + (++secuencia)).fechaComprobante(fecha)
                .lineas(List.of(lineas)).moneda(MonedaFE.CRC).build();
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
