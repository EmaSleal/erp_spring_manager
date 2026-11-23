package api.astro.whats_orders_manager.modules.proveedores.it;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.DetalleOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-053 revisión 1 (criterios 15, 16 y 17): fecha de vencimiento de toda CxP, cancelar una OC con facturas y GET de la OC como DTO.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-053 revisión 1 — vencimiento de la CxP, cancelar con facturas y DTO de la OC (MySQL)")
class OrdenCompraRevision1IT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t053r", "t053-it-mysql-rev1");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    private static final String USUARIO = "admin.bootstrap@example.invalid";
    private static final LocalDate FECHA = LocalDate.now().minusDays(60);

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;
    @MockitoBean private PermisoService permisoService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private OrdenCompraRepository ocRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;

    private TransactionTemplate tx;
    private Long mercaderiaId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        when(permisoService.tienePermisoPorCodigo(any(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("lector"), eq("ORDEN_COMPRA_VER"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("contador"), eq("CUENTA_PAGAR_VER"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq(USUARIO), eq("ORDEN_COMPRA_CANCELAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq(USUARIO), eq("CUENTA_PAGAR_ANULAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("contador"), eq("CUENTA_PAGAR_CREAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("contador"), eq("ORDEN_COMPRA_VER"))).thenReturn(true);
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave("CPP_CUENTA_POR_PAGAR").isEmpty()) {
                param("CPP_CUENTA_COMPRAS", cuenta("9.5.04.001", "Compras", TipoCuenta.EGRESO));
                param("CPP_CUENTA_POR_PAGAR", cuenta("9.2.05.001", "Proveedores", TipoCuenta.PASIVO));
                param("CPP_IVA_CREDITO_FISCAL", cuenta("9.1.04.001", "IVA crédito", TipoCuenta.ACTIVO));
            }
            mercaderiaId = cuentaRepository.findByCodigo("9.5.04.002").map(CuentaContable::getIdCuenta)
                    .orElseGet(() -> cuenta("9.5.04.002", "Compras mercadería", TipoCuenta.EGRESO).getIdCuenta());
            if (parametroRepository.findByClave("COMPRAS_MERCADERIA").isEmpty()) {
                param("COMPRAS_MERCADERIA", cuentaRepository.findById(mercaderiaId).orElseThrow());
            }
        });
    }

    // ── Criterio 15: fecha de vencimiento ────────────────────────────────────

    @Test
    @DisplayName("15: desde OC con proveedor de 30 días y sin fechaVencimiento: comprobante + 30; /vencidas la lista")
    void vencimientoConDiasDeCredito() throws Exception {
        Proveedor p = proveedor(30);
        OrdenCompra oc = ordenCompra(p, EstadoOrdenCompra.RECIBIDA);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoOc(oc, "FE-V30", null)))
                .andExpect(status().isOk());
        assertThat(vencimiento("FE-V30", p)).isEqualTo(FECHA.plusDays(30));
        mockMvc.perform(get("/api/cuentas-pagar/vencidas").with(user("contador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.numeroComprobanteProveedor=='FE-V30')]").isNotEmpty());
    }

    @Test
    @DisplayName("15: manual con proveedor de 30 días y sin fechaVencimiento: comprobante + 30")
    void vencimientoManualConDiasDeCredito() throws Exception {
        Proveedor p = proveedor(30);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoManual(p, "FE-M30", null)))
                .andExpect(status().isOk());
        assertThat(vencimiento("FE-M30", p)).isEqualTo(FECHA.plusDays(30));
    }

    @Test
    @DisplayName("15: proveedor sin días de crédito (0 o nulo): vence el día del comprobante")
    void vencimientoContado() throws Exception {
        Proveedor cero = proveedor(0);
        Proveedor nulo = proveedor(null);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoManual(cero, "FE-C0", null)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoManual(nulo, "FE-CN", null)))
                .andExpect(status().isOk());
        assertThat(vencimiento("FE-C0", cero)).isEqualTo(FECHA);
        assertThat(vencimiento("FE-CN", nulo)).isEqualTo(FECHA);
    }

    @Test
    @DisplayName("15: fecha explícita válida se respeta; anterior al comprobante: 400 con mensaje fijo y sin CxP")
    void vencimientoExplicito() throws Exception {
        Proveedor p = proveedor(30);
        LocalDate explicita = FECHA.plusDays(10);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoManual(p, "FE-X1", explicita.toString())))
                .andExpect(status().isOk());
        assertThat(vencimiento("FE-X1", p)).isEqualTo(explicita);

        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoManual(p, "FE-X2", FECHA.minusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de vencimiento no puede ser anterior a la fecha del comprobante"));
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE numero_comprobante_proveedor = 'FE-X2'", Integer.class);
        assertThat(n).isZero();
    }

    // ── Criterio 16: cancelar con facturas ───────────────────────────────────

    @Test
    @DisplayName("16: cancelar una OC con CxP no anulada: 409 con mensaje fijo y la OC sigue igual")
    void cancelarConFacturas() throws Exception {
        Proveedor p = proveedor(0);
        OrdenCompra oc = ordenCompra(p, EstadoOrdenCompra.RECIBIDA_PARCIAL);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoOc(oc, "FE-K1", null)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ordenes-compra/" + oc.getId() + "/cancelar").with(user(USUARIO)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La orden de compra tiene facturas registradas; anúlelas antes de cancelarla"));
        assertThat(ocRepository.findById(oc.getId()).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA_PARCIAL);
    }

    @Test
    @DisplayName("16: RECIBIDA_PARCIAL con su única CxP anulada se cancela; sin CxP el comportamiento no cambia")
    void cancelarSinFacturasVigentes() throws Exception {
        Proveedor p = proveedor(0);
        OrdenCompra oc = ordenCompra(p, EstadoOrdenCompra.RECIBIDA_PARCIAL);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoOc(oc, "FE-K2", null)))
                .andExpect(status().isOk());
        jdbc.update("UPDATE cuentas_por_pagar SET estado = 'ANULADA' WHERE orden_compra_id = ?", oc.getId());
        mockMvc.perform(post("/api/ordenes-compra/" + oc.getId() + "/cancelar").with(user(USUARIO)).with(csrf()))
                .andExpect(status().isOk());
        assertThat(ocRepository.findById(oc.getId()).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.CANCELADA);

        OrdenCompra sinCxp = ordenCompra(p, EstadoOrdenCompra.APROBADA);
        mockMvc.perform(post("/api/ordenes-compra/" + sinCxp.getId() + "/cancelar").with(user(USUARIO)).with(csrf()))
                .andExpect(status().isOk());
        assertThat(ocRepository.findById(sinCxp.getId()).orElseThrow().getEstado()).isEqualTo(EstadoOrdenCompra.CANCELADA);
    }

    // ── Criterio 17: GET de la OC como DTO ───────────────────────────────────

    @Test
    @DisplayName("17: GET /api/ordenes-compra/{id} y el listado con solo ORDEN_COMPRA_VER: 200 con DTO (proveedor y detalles diferidos)")
    void getOrdenComoDto() throws Exception {
        Proveedor p = proveedor(0);
        OrdenCompra oc = ordenCompra(p, EstadoOrdenCompra.APROBADA);
        mockMvc.perform(get("/api/ordenes-compra/" + oc.getId()).with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numero").value(oc.getNumero()))
                .andExpect(jsonPath("$.data.proveedorId").value(p.getId()))
                .andExpect(jsonPath("$.data.proveedorNombre").value(p.getNombre()))
                .andExpect(jsonPath("$.data.estado").value("APROBADA"))
                .andExpect(jsonPath("$.data.detalles.length()").value(1))
                .andExpect(jsonPath("$.data.detalles[0].descripcion").value("Resina"))
                .andExpect(jsonPath("$.data.detalles[0].cantidad").value(200));
        mockMvc.perform(get("/api/ordenes-compra").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==" + oc.getId() + ")].proveedorNombre").value(p.getNombre()));
        mockMvc.perform(get("/api/ordenes-compra?estado=APROBADA").with(user("lector")))
                .andExpect(status().isOk());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private LocalDate vencimiento(String comprobante, Proveedor p) {
        return jdbc.queryForObject("SELECT fecha_vencimiento FROM cuentas_por_pagar WHERE proveedor_id = ? AND numero_comprobante_proveedor = ?",
                LocalDate.class, p.getId(), comprobante);
    }

    private Map<String, Object> base(String numero, String vencimiento) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("descripcion", "Línea de prueba");
        fila.put("cuentaContableId", mercaderiaId);
        fila.put("codigoTarifa", "08");
        fila.put("base", "100.00");
        fila.put("iva", "13.00");
        fila.put("tratamiento", "PLENO");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("descripcion", "Factura de prueba");
        m.put("numeroComprobanteProveedor", numero);
        m.put("fechaComprobante", FECHA.toString());
        if (vencimiento != null) {
            m.put("fechaVencimiento", vencimiento);
        }
        m.put("lineas", new ArrayList<>(List.of(fila)));
        return m;
    }

    private String cuerpoOc(OrdenCompra oc, String numero, String vencimiento) throws Exception {
        Map<String, Object> m = base(numero, vencimiento);
        m.put("ordenCompraId", oc.getId());
        return objectMapper.writeValueAsString(m);
    }

    private String cuerpoManual(Proveedor p, String numero, String vencimiento) throws Exception {
        Map<String, Object> m = base(numero, vencimiento);
        m.put("proveedorId", p.getId());
        return objectMapper.writeValueAsString(m);
    }

    private OrdenCompra ordenCompra(Proveedor p, EstadoOrdenCompra estado) {
        return tx.execute(s -> {
            OrdenCompra o = OrdenCompra.builder().numero("OC-9-" + UUID.randomUUID().toString().substring(0, 8))
                    .proveedor(p).estado(estado).fecha(FECHA).impuesto(new BigDecimal("13000.00")).build();
            o.getDetalles().add(DetalleOrdenCompra.builder().ordenCompra(o).descripcion("Resina").cantidad(200)
                    .precioUnitario(new BigDecimal("500.00")).total(new BigDecimal("100000.00")).build());
            o.recalcularTotales();
            return ocRepository.save(o);
        });
    }

    private Proveedor proveedor(Integer diasCredito) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Proveedor p = Proveedor.builder().cedula("31" + sufijo).nombre("Proveedor " + sufijo)
                .tipoProveedor(TipoProveedor.values()[0]).build();
        p.setDiasCredito(diasCredito);
        return proveedorRepository.save(p);
    }

    private CuentaContable cuenta(String codigo, String nombre, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setNivel(codigo.split("\\.").length);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
