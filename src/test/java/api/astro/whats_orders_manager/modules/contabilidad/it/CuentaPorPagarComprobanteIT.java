package api.astro.whats_orders_manager.modules.contabilidad.it;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorDuplicadoException;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorValidator;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * T-052 (ADR-066 §1-3) de punta a punta sobre MySQL (Testcontainers, esquema de Hibernate): alta con comprobante y
 * desglose, errores 400/409 con mensajes fijos, duplicado concurrente (el índice único y el bloqueo del proveedor dejan
 * pasar solo una), anulación con permiso nuevo, pagos y DTO sin entidades.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-052 — comprobante del proveedor, duplicados y anulación de CxP (MySQL)")
class CuentaPorPagarComprobanteIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t052c", "t052-it-mysql-comprobante");

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
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private CuentaPorPagarService cxpService;
    @Autowired private api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService usuarioService;

    private TransactionTemplate tx;
    private Long gastoId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        // Permisos: el usuario «contador» crea y ve; «anulador» además anula; «lector» solo ve
        when(permisoService.tienePermisoPorCodigo(any(), anyString())).thenReturn(false);
        for (String u : List.of("lector", "contador", "anulador", USUARIO)) {
            when(permisoService.tienePermisoPorCodigo(eq(u), eq("CUENTA_PAGAR_VER"))).thenReturn(true);
        }
        for (String u : List.of("contador", "anulador", USUARIO)) {
            when(permisoService.tienePermisoPorCodigo(eq(u), eq("CUENTA_PAGAR_CREAR"))).thenReturn(true);
        }
        for (String u : List.of("anulador", USUARIO)) {
            when(permisoService.tienePermisoPorCodigo(eq(u), eq("CUENTA_PAGAR_ANULAR"))).thenReturn(true);
        }
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave("CPP_CUENTA_POR_PAGAR").isEmpty()) {
                param("CPP_CUENTA_COMPRAS", cuenta("9.5.04.001", "Compras", TipoCuenta.EGRESO));
                param("CPP_CUENTA_POR_PAGAR", cuenta("9.2.05.001", "Proveedores", TipoCuenta.PASIVO));
                param("CPP_IVA_CREDITO_FISCAL", cuenta("9.1.04.001", "IVA crédito", TipoCuenta.ACTIVO));
            }
            gastoId = cuentaRepository.findByCodigo("9.5.04.001").map(CuentaContable::getIdCuenta)
                    .orElseGet(() -> cuenta("9.5.04.001", "Compras", TipoCuenta.EGRESO).getIdCuenta());
        });
    }

    // ── Alta ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST crea la CxP con número, clave, fecha y desglose y responde el DTO con los avisos")
    void crear_ok() throws Exception {
        Proveedor p = proveedor();
        Map<String, Object> body = cuerpo(p, "FE-100", "100000.00", "13000.00");
        body.put("claveComprobante", "5".repeat(50));
        ((List<Map<String, Object>>) body.get("lineas")).get(0).put("iva", "12000.00");

        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.numeroComprobanteProveedor").value("FE-100"))
                .andExpect(jsonPath("$.data.cuenta.claveComprobante").value("5".repeat(50)))
                .andExpect(jsonPath("$.data.cuenta.fechaEmisionComprobante").value("2026-10-20"))
                .andExpect(jsonPath("$.data.cuenta.fechaEmision").value("2026-10-20"))
                .andExpect(jsonPath("$.data.cuenta.proveedorNombre").value(p.getNombre()))
                .andExpect(jsonPath("$.data.cuenta.lineas[0].codigoTarifa").value("08"))
                .andExpect(jsonPath("$.data.cuenta.lineas[0].porcentaje").value(13))
                .andExpect(jsonPath("$.data.cuenta.lineas[0].tratamiento").value("PLENO"))
                .andExpect(jsonPath("$.data.cuenta.lineas[0].cuentaCodigo").value("9.5.04.001"))
                .andExpect(jsonPath("$.data.cuenta.estadoAceptacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.advertencias.length()").value(1));
    }

    @Test
    @DisplayName("validaciones: 400 con mensaje fijo (número, fecha futura, clave, líneas, base)")
    void crear_errores400() throws Exception {
        Proveedor p = proveedor();
        Map<String, Object> sinNumero = cuerpo(p, "", "100.00", "13.00");
        Map<String, Object> futura = cuerpo(p, "FE-F", "100.00", "13.00");
        futura.put("fechaComprobante", LocalDate.now().plusDays(2).toString());
        Map<String, Object> clave = cuerpo(p, "FE-C", "100.00", "13.00");
        clave.put("claveComprobante", "123");
        Map<String, Object> sinFilas = cuerpo(p, "FE-S", "100.00", "13.00");
        sinFilas.put("lineas", List.of());
        Map<String, Object> sinBase = cuerpo(p, "FE-M", "0.00", "0.00");

        esperar400(sinNumero, "El número del comprobante del proveedor es obligatorio");
        esperar400(futura, "La fecha de emisión del comprobante no puede ser futura");
        esperar400(clave, "La clave del comprobante debe tener 50 dígitos");
        esperar400(sinFilas, "La cuenta por pagar debe tener al menos una línea");
        esperar400(sinBase, ComprobanteProveedorValidator.MSG_LINEA);
    }

    @Test
    @DisplayName("duplicado: 409 «Ya existe una cuenta por pagar de este proveedor con el comprobante …»; tras anular, se permite")
    void crear_duplicado409YPermitidoTrasAnular() throws Exception {
        Proveedor p = proveedor();
        mockMvc.perform(post("/api/cuentas-pagar").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo(p, "FE-200", "1000.00", "130.00"))))
                .andExpect(status().isOk());
        Map<String, Object> repetido = cuerpo(p, " fe-200", "1000.00", "130.00");
        mockMvc.perform(post("/api/cuentas-pagar").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(repetido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Ya existe una cuenta por pagar de este proveedor con el comprobante fe-200"));

        Long id = jdbc.queryForObject("SELECT id FROM cuentas_por_pagar WHERE proveedor_id = ?", Long.class, p.getId());
        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/anular").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Duplicada por error\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cuentas-pagar").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(repetido)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("concurrencia: dos hilos crean el mismo comprobante del mismo proveedor → exactamente uno gana y el otro recibe el duplicado")
    void concurrencia_soloUnoGana() throws Exception {
        Proveedor p = proveedor();
        int hilos = 2;
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<Object>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit((Callable<Object>) () -> {
                listos.countDown();
                salida.await();
                try {
                    return cxpService.crear(dto(p, "FE-CONC", "1000.00", "130.00"));
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        listos.await();
        salida.countDown();
        List<Object> resultados = new ArrayList<>();
        for (Future<Object> f : futuros) {
            resultados.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        long exitos = resultados.stream().filter(r -> !(r instanceof Exception)).count();
        List<Object> fallos = resultados.stream().filter(r -> r instanceof Exception).toList();
        assertThat(exitos).isEqualTo(1);
        assertThat(fallos).hasSize(1).first().isInstanceOf(ComprobanteProveedorDuplicadoException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE proveedor_id = ?", Integer.class,
                p.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT saldo_pendiente FROM proveedores WHERE id = ?", BigDecimal.class,
                p.getId())).isEqualByComparingTo("1130.00");
    }

    // ── Anulación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("anular: 403 sin CUENTA_PAGAR_ANULAR; 400 con motivo corto; 200; 409 si ya está anulada; el detalle muestra ANULADA y el motivo")
    void anular_endpoint() throws Exception {
        Proveedor p = proveedor();
        Long id = crearYObtenerId(p, "FE-300");

        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/anular").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Registrada por error\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/anular").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El motivo de anulación debe tener entre 3 y 180 caracteres"));
        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/anular").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Registrada por error\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/anular").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Registrada por error\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La cuenta por pagar ya está anulada"));
        mockMvc.perform(get("/api/cuentas-pagar/" + id).with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ANULADA"))
                .andExpect(jsonPath("$.data.motivoAnulacion").value("Registrada por error"))
                .andExpect(jsonPath("$.data.fechaAnulacion").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT saldo_pendiente FROM proveedores WHERE id = ?", BigDecimal.class,
                p.getId())).isEqualByComparingTo("0.00");
        // el asiento original sigue contabilizado y existe su reversión
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE cuenta_por_pagar_id = ?",
                Integer.class, id)).isEqualTo(2);
    }

    @Test
    @DisplayName("anular con pagos activos: 409 con mensaje fijo y nada cambia; GET pagos devuelve DTO")
    void anular_conPagos409() throws Exception {
        Proveedor p = proveedor();
        Long id = crearYObtenerId(p, "FE-400");
        jdbc.update("INSERT INTO pagos_proveedor (numero, cuenta_por_pagar_id, monto, moneda, tipo_cambio, monto_aplicado, "
                + "metodo_pago, fecha, estado) VALUES ('PP-2026-90001', ?, 100.00, 'CRC', 1, 100.00, ?, '2026-10-21', "
                + "'CONFIRMADO')", id, MetodoPagoProveedor.EFECTIVO.name());

        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/anular").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Registrada por error\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("La cuenta por pagar tiene pagos activos; anule los pagos antes de anular la cuenta"));
        assertThat(jdbc.queryForObject("SELECT estado FROM cuentas_por_pagar WHERE id = ?", String.class, id))
                .isEqualTo("PENDIENTE");
        mockMvc.perform(get("/api/cuentas-pagar/" + id + "/pagos").with(user("lector")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].numero").value("PP-2026-90001"));
        mockMvc.perform(get("/api/cuentas-pagar/pagos").param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .with(user("lector")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].numero").value("PP-2026-90001"))
                .andExpect(jsonPath("$.data[0].cuentaPorPagarNumero").isNotEmpty());
    }

    @Test
    @DisplayName("T-052 anulación concurrente: dos hilos anulan la misma CxP → una gana, la otra recibe el 409 fijo, una sola reversión")
    void anulacionConcurrente_soloUnaGana() throws Exception {
        Proveedor p = proveedor();
        Long id = crearYObtenerId(p, "FE-RACE");
        var usuario = usuarioService.findByEmail(USUARIO).orElseThrow();
        int hilos = 2;
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<Object>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit((Callable<Object>) () -> {
                listos.countDown();
                salida.await();
                try {
                    return cxpService.anular(id, "Anulación simultánea", usuario);
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        listos.await();
        salida.countDown();
        List<Object> resultados = new ArrayList<>();
        for (Future<Object> f : futuros) {
            resultados.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(resultados.stream().filter(r -> !(r instanceof Exception)).count()).isEqualTo(1);
        List<Object> fallos = resultados.stream().filter(r -> r instanceof Exception).toList();
        assertThat(fallos).hasSize(1);
        assertThat(fallos.get(0)).isInstanceOf(IllegalStateException.class);
        assertThat(((Exception) fallos.get(0)).getMessage()).isEqualTo("La cuenta por pagar ya está anulada");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE cuenta_por_pagar_id = ?",
                Integer.class, id)).as("original + una reversión").isEqualTo(2);
    }

    // ── T-054: líneas con tratamiento, prorrata y aceptación del comprobante ──

    @Test
    @DisplayName("T-054 USO_COMUN sin prorrata del año: 409 con el año; con la prorrata cargada se crea y la línea guarda el porcentaje")
    void usoComun_prorrata409YLuegoOk() throws Exception {
        Proveedor p = proveedor();
        jdbc.update("DELETE FROM prorrata_iva WHERE anio = 2026");
        Map<String, Object> body = cuerpo(p, "FE-UC", "1000.00", "130.00");
        ((List<Map<String, Object>>) body.get("lineas")).get(0).put("tratamiento", "USO_COMUN");

        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay porcentaje de prorrata configurado para 2026"));

        jdbc.update("INSERT INTO prorrata_iva (anio, porcentaje, fecha_registro, fecha_actualizacion) VALUES (2026, 80.00, NOW(), NOW())");
        try {
            mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cuenta.lineas[0].tratamiento").value("USO_COMUN"))
                    .andExpect(jsonPath("$.data.cuenta.lineas[0].porcentajeProrrata").value(80.0))
                    .andExpect(jsonPath("$.data.cuenta.lineas[0].ivaAcreditable").value(104.0))
                    .andExpect(jsonPath("$.data.cuenta.lineas[0].ivaNoAcreditable").value(26.0));
        } finally {
            jdbc.update("DELETE FROM prorrata_iva WHERE anio = 2026");
        }
    }

    @Test
    @DisplayName("T-054 compra al 13 % limitada al 1 %: acreditable 10,00 y no acreditable 120,00 en la base, y el asiento lo suma a la cuenta")
    void limitado_enMysql() throws Exception {
        Proveedor p = proveedor();
        Map<String, Object> body = cuerpo(p, "FE-LIM", "1000.00", "130.00");
        Map<String, Object> linea = ((List<Map<String, Object>>) body.get("lineas")).get(0);
        linea.put("tratamiento", "LIMITADO");
        linea.put("tarifaLimite", "1");

        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.lineas[0].ivaAcreditable").value(10.0))
                .andExpect(jsonPath("$.data.cuenta.lineas[0].ivaNoAcreditable").value(120.0));
        Long id = jdbc.queryForObject("SELECT id FROM cuentas_por_pagar WHERE proveedor_id = ?", Long.class, p.getId());
        assertThat(jdbc.queryForObject("SELECT iva_acreditable FROM cuenta_pagar_linea WHERE cuenta_por_pagar_id = ?",
                BigDecimal.class, id)).isEqualByComparingTo("10.00");
        assertThat(jdbc.queryForObject("SELECT SUM(d.debe) FROM detalles_asiento d JOIN asientos_contables a ON a.id = d.asiento_id "
                + "WHERE a.cuenta_por_pagar_id = ?", BigDecimal.class, id)).isEqualByComparingTo("1130.00");
    }

    @Test
    @DisplayName("T-054 aceptación: 403 sin permiso de CxP, 400 con fecha futura, 200 y auditoría; el listado filtra; 409 si está anulada")
    void aceptacion_endpoint() throws Exception {
        Proveedor p = proveedor();
        Long id = crearYObtenerId(p, "FE-ACE");
        String valida = "{\"estado\":\"ACEPTADO\",\"fecha\":\"" + FECHA.plusDays(1) + "\",\"numero\":\"MR-0001\"}";

        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/aceptacion").with(user("lector")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(valida))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/aceptacion").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACEPTADO\",\"fecha\":\"" + LocalDate.now().plusDays(3) + "\",\"numero\":\"MR-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de aceptación no puede ser futura"));
        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/aceptacion").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACEPTADO\",\"fecha\":\"" + FECHA.minusDays(1) + "\",\"numero\":\"MR-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de aceptación no puede ser anterior a la fecha del comprobante"));
        mockMvc.perform(get("/api/cuentas-pagar").param("aceptacion", "PENDIENTE").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")].diasDesdeComprobante").isNotEmpty());

        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/aceptacion").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(valida))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT estado_aceptacion FROM cuentas_por_pagar WHERE id = ?", String.class, id)).isEqualTo("ACEPTADO");
        assertThat(jdbc.queryForObject("SELECT numero_mensaje_receptor FROM cuentas_por_pagar WHERE id = ?", String.class, id)).isEqualTo("MR-0001");
        mockMvc.perform(get("/api/cuentas-pagar").param("aceptacion", "ACEPTADO").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")].estadoAceptacion").value("ACEPTADO"))
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")].diasDesdeComprobante")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
        mockMvc.perform(get("/api/cuentas-pagar").param("aceptacion", "PENDIENTE").with(user("lector")))
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").isEmpty());

        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/anular").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Registrada por error\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cuentas-pagar/" + id + "/aceptacion").with(user(USUARIO)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(valida))
                .andExpect(status().isConflict());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void esperar400(Map<String, Object> body, String mensaje) throws Exception {
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    private Long crearYObtenerId(Proveedor p, String numero) throws Exception {
        mockMvc.perform(post("/api/cuentas-pagar").with(user("contador")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo(p, numero, "1000.00", "130.00"))))
                .andExpect(status().isOk());
        return jdbc.queryForObject("SELECT id FROM cuentas_por_pagar WHERE proveedor_id = ? AND numero_comprobante_proveedor = ?",
                Long.class, p.getId(), numero);
    }

    private Map<String, Object> cuerpo(Proveedor p, String numero, String base, String iva) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("descripcion", "Línea de prueba");
        fila.put("cuentaContableId", gastoId);
        fila.put("codigoTarifa", "08");
        fila.put("base", base);
        fila.put("iva", iva);
        fila.put("tratamiento", "PLENO");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("proveedorId", p.getId());
        m.put("descripcion", "Servicio de prueba");
        m.put("numeroComprobanteProveedor", numero);
        m.put("fechaComprobante", FECHA.toString());
        m.put("moneda", "CRC");
        m.put("lineas", new ArrayList<>(List.of(fila)));
        return m;
    }

    private CuentaPorPagarDTO dto(Proveedor p, String numero, String base, String iva) {
        return CuentaPorPagarDTO.builder().proveedorId(p.getId()).descripcion("Servicio de prueba")
                .numeroComprobanteProveedor(numero).fechaComprobante(FECHA)
                .moneda(MonedaFE.CRC)
                .lineas(List.of(new CuentaPorPagarLineaDTO("Línea de prueba", gastoId, "08", new BigDecimal(base),
                        new BigDecimal(iva), TratamientoIvaCompra.PLENO, null))).build();
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
