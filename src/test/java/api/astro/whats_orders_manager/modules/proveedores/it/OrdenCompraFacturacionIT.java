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
 * T-053 de punta a punta por la API sobre MySQL: GET /api/ordenes-compra/{id}/facturacion, alta de CxP con
 * ordenCompraId (comprobante y líneas), advertencia de exceso sin bloquear, 409 por estado y duplicado, 400 por
 * proveedor distinto, ruta vieja inexistente y cuenta propuesta opcional.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-053 — OC: facturación por la API (MySQL)")
class OrdenCompraFacturacionIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t053o", "t053-it-mysql-oc");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    private static final String USUARIO = "admin.bootstrap@example.invalid";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

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

    @Test
    @DisplayName("dos facturas por la API: facturado y pendiente en GET /facturacion; el exceso se advierte sin bloquear; el comprobante repetido da 409")
    void dosFacturas_excesoYDuplicado() throws Exception {
        Proveedor p = proveedor();
        OrdenCompra oc = ordenCompra(p, EstadoOrdenCompra.RECIBIDA_PARCIAL);

        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(oc, null, "FE-1", "50000.00", "6500.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.estadoAceptacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.cuenta.fechaEmision").value("2026-10-20"))
                .andExpect(jsonPath("$.data.advertencias.length()").value(0));
        mockMvc.perform(get("/api/ordenes-compra/" + oc.getId() + "/facturacion").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(113000.0))
                .andExpect(jsonPath("$.data.facturado").value(56500.0))
                .andExpect(jsonPath("$.data.pendiente").value(56500.0))
                .andExpect(jsonPath("$.data.exceso").value(0.0))
                .andExpect(jsonPath("$.data.cuentaPropuestaId").value(mercaderiaId))
                .andExpect(jsonPath("$.data.cuentas.length()").value(1))
                .andExpect(jsonPath("$.data.cuentas[0].numeroComprobanteProveedor").value("FE-1"));

        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(oc, null, "FE-2", "100000.00", "13000.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.advertencias.length()").value(1))
                .andExpect(jsonPath("$.data.advertencias[0]").value("Lo facturado (169500.00) supera el total de la orden de compra "
                        + oc.getNumero() + " (113000.00) por 56500.00"));
        mockMvc.perform(get("/api/ordenes-compra/" + oc.getId() + "/facturacion").with(user("lector")))
                .andExpect(jsonPath("$.data.exceso").value(56500.0))
                .andExpect(jsonPath("$.data.pendiente").value(0.0));

        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(oc, null, "FE-1", "10.00", "1.30")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("OC en estado que no admite facturar: 409 con mensaje fijo; proveedor distinto: 400; sin comprobante: 400")
    void errores() throws Exception {
        Proveedor p = proveedor();
        Proveedor otro = proveedor();
        OrdenCompra aprobada = ordenCompra(p, EstadoOrdenCompra.APROBADA);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(aprobada, null, "FE-9", "100.00", "13.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La orden de compra no admite facturas en estado APROBADA; "
                        + "debe estar recibida total o parcialmente"));

        OrdenCompra recibida = ordenCompra(p, EstadoOrdenCompra.RECIBIDA);
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(recibida, otro.getId(), "FE-8", "100.00", "13.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El proveedor de la cuenta por pagar debe ser el de la orden de compra"));
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(recibida, null, "", "100.00", "13.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("la ruta vieja POST /api/ordenes-compra/{id}/generar-cpp ya no existe")
    void rutaViejaNoExiste() throws Exception {
        Proveedor p = proveedor();
        OrdenCompra oc = ordenCompra(p, EstadoOrdenCompra.RECIBIDA);
        int sinCxp = jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE orden_compra_id = ?", Integer.class, oc.getId());
        mockMvc.perform(post("/api/ordenes-compra/" + oc.getId() + "/generar-cpp").with(user("contador")).with(csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(r -> {
                    int s = r.getResponse().getStatus();
                    if (s != 404 && s != 405) {
                        throw new AssertionError("se esperaba 404 o 405 y fue " + s);
                    }
                });
        int despues = jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE orden_compra_id = ?", Integer.class, oc.getId());
        if (sinCxp != despues) {
            throw new AssertionError("la ruta vieja creó una CxP");
        }
    }

    @Test
    @DisplayName("sin el parámetro COMPRAS_MERCADERIA: la cuenta propuesta es null y se puede facturar igual")
    void sinParametro() throws Exception {
        Proveedor p = proveedor();
        OrdenCompra oc = ordenCompra(p, EstadoOrdenCompra.RECIBIDA);
        ParametroContable guardado = parametroRepository.findByClave("COMPRAS_MERCADERIA").orElseThrow();
        Long cuentaId = guardado.getCuentaContable().getIdCuenta();
        parametroRepository.delete(guardado);
        try {
            mockMvc.perform(get("/api/ordenes-compra/" + oc.getId() + "/facturacion").with(user("lector")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cuentaPropuestaId").doesNotExist());
            mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo(oc, null, "FE-SP", "100.00", "13.00")))
                    .andExpect(status().isOk());
        } finally {
            param("COMPRAS_MERCADERIA", cuentaRepository.findById(cuentaId).orElseThrow());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String cuerpo(OrdenCompra oc, Long proveedorId, String numero, String base, String iva) throws Exception {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("descripcion", "Línea de prueba");
        fila.put("cuentaContableId", mercaderiaId);
        fila.put("codigoTarifa", "08");
        fila.put("base", base);
        fila.put("iva", iva);
        fila.put("tratamiento", "PLENO");
        Map<String, Object> m = new LinkedHashMap<>();
        if (proveedorId != null) {
            m.put("proveedorId", proveedorId);
        }
        m.put("ordenCompraId", oc.getId());
        m.put("descripcion", "Factura de la orden");
        m.put("numeroComprobanteProveedor", numero);
        m.put("fechaComprobante", FECHA.toString());
        m.put("lineas", new ArrayList<>(List.of(fila)));
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

    private Proveedor proveedor() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        return proveedorRepository.save(Proveedor.builder().cedula("31" + sufijo).nombre("Proveedor " + sufijo)
                .tipoProveedor(TipoProveedor.values()[0]).build());
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
