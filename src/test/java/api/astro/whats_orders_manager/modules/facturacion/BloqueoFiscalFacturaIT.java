package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * T-025 (ADR-030, P-048) — los campos fiscales de una factura no se pueden modificar mientras su comprobante
 * electrónico vigente está en Hacienda (ENVIADO, PROCESANDO, ACEPTADO) o pudo haber llegado (ENVIO_INCIERTO).
 *
 * Cada caso crea una factura con dos líneas por la ruta de la UI (asiento de venta incluido), le asocia un
 * comprobante en el estado indicado y prueba cada ruta que modifica la factura: si la política la bloquea, la
 * respuesta es 409 y la factura, sus líneas y el asiento vigente quedan iguales.
 *
 * Base: MySQL 8 en Testcontainers (t025-it-mysql, tmpfs, sin volúmenes); esquema de Hibernate y rutinas de
 * database/routines, como en LineaFacturaSincronizacionIT. Ninguna prueba llama a Hacienda.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-025 — bloqueo fiscal de facturas según el comprobante electrónico (MySQL)")
class BloqueoFiscalFacturaIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t025")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t025-it-mysql"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("database/routines/stored_procedures.sql")),
                    "/docker-entrypoint-initdb.d/01-routines.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    /** Administrador de bootstrap que DataInitializer crea en la base de pruebas. */
    private static final String USUARIO = "admin.bootstrap@example.invalid";

    /** Estados del comprobante vigente que bloquean los campos fiscales (ADR-030 §2). */
    private static final Set<String> BLOQUEANTES = Set.of("ENVIADO", "PROCESANDO", "ACEPTADO");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private LineaFacturaRepository lineaFacturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;

    private TransactionTemplate tx;
    private Producto gravado13;
    private Producto exento;
    private Integer idEmpresa;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES).isEmpty()) {
                param(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES,
                        cuenta("9.1.02.001", "Clientes", TipoCuenta.ACTIVO));
                param(AsientoContableService.CLAVE_VENTA_INGRESOS,
                        cuenta("9.4.01.001", "Ventas", TipoCuenta.INGRESO));
                param(AsientoContableService.CLAVE_VENTA_IVA_POR_PAGAR,
                        cuenta("9.2.01.002", "IVA por pagar", TipoCuenta.PASIVO));
            }
            idEmpresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                    .orElseGet(() -> empresaRepository.save(ConfiguracionEmpresa.builder()
                            .razonSocial("Empresa T025 S.A.").build()))
                    .getIdConfiguracion();
            gravado13 = producto("Producto A gravado 13 %", "1130.00", true);
            exento = producto("Producto B exento", "500.00", false);
        });
    }

    // ── Líneas: PUT /lineas-factura/actualizar ───────────────────────────────

    @ParameterizedTest(name = "comprobante {0}: quitar una línea")
    @ValueSource(strings = {"SIN_COMPROBANTE", "GENERADO", "FIRMADO", "ERROR", "RECHAZADO",
            "ENVIADO", "PROCESANDO", "ACEPTADO"})
    @DisplayName("PUT /lineas-factura/actualizar: 200 si el estado lo permite; 409 sin cambios si bloquea")
    void lineas_segunEstado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = guardarLineas(List.of(linea(antes.lineas().get(0).id(), idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus();

        if (BLOQUEANTES.contains(estado)) {
            assertThat(status).isEqualTo(409);
            assertThat(estado(idFactura)).isEqualTo(antes);
        } else {
            assertThat(status).isEqualTo(200);
            assertThat(lineas(idFactura)).hasSize(1);
            assertThat(totales(idFactura).total()).isEqualByComparingTo("1130.00");
        }
    }

    // ── Factura: POST /facturas/guardar (edición) ────────────────────────────

    @ParameterizedTest(name = "comprobante {0}: cambiar la condición de venta")
    @ValueSource(strings = {"SIN_COMPROBANTE", "GENERADO", "FIRMADO", "ERROR", "RECHAZADO",
            "ENVIADO", "PROCESANDO", "ACEPTADO"})
    @DisplayName("POST /facturas/guardar con idFactura: 200 si el estado lo permite; 409 sin cambios si bloquea")
    void guardarFactura_segunEstado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = mockMvc.perform(post("/facturas/guardar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facturaEditada(idFactura, "CREDITO"))))
                .andReturn().getResponse().getStatus();

        if (BLOQUEANTES.contains(estado)) {
            assertThat(status).isEqualTo(409);
            assertThat(estado(idFactura)).isEqualTo(antes);
            assertThat(condicionVenta(idFactura)).isNotEqualTo("CREDITO");
        } else {
            assertThat(status).isEqualTo(200);
            assertThat(condicionVenta(idFactura)).isEqualTo("CREDITO");
        }
    }

    // ── Eliminar y anular ────────────────────────────────────────────────────

    @ParameterizedTest(name = "comprobante {0}: eliminar")
    @ValueSource(strings = {"ENVIADO", "PROCESANDO", "ACEPTADO"})
    @DisplayName("GET /facturas/eliminar/{id} con comprobante en Hacienda → 409 y la factura sigue")
    void eliminar_bloqueado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = mockMvc.perform(get("/facturas/eliminar/" + idFactura)
                        .with(user(USUARIO).roles("ADMIN")).accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(409);
        assertThat(estado(idFactura)).isEqualTo(antes);
        assertThat(comprobantes(idFactura)).isEqualTo(1);
    }

    @ParameterizedTest(name = "comprobante {0}: anular")
    @ValueSource(strings = {"ENVIADO", "PROCESANDO", "ACEPTADO"})
    @DisplayName("POST /facturas/anular/{id} con comprobante en Hacienda → 409 y la factura no se anula")
    void anular_bloqueado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = mockMvc.perform(post("/facturas/anular/" + idFactura)
                        .param("motivo", "Prueba T-025")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()).accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(409);
        assertThat(estado(idFactura)).isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .isFalse();
    }

    // ── Campo operativo ──────────────────────────────────────────────────────

    @Test
    @DisplayName("con comprobante ACEPTADO, cambiar observaciones (PUT /facturas/actualizar-estado) → 200")
    void campoOperativo_conAceptado_permitido() throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, "ACEPTADO");

        int status = mockMvc.perform(put("/facturas/actualizar-estado/" + idFactura)
                        .param("entregado", "true").param("descripcion", "Observación interna T-025")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT descripcion FROM factura WHERE id_factura = ?", String.class,
                idFactura)).isEqualTo("Observación interna T-025");
    }

    // ── Listener JPA en LineaFactura ─────────────────────────────────────────

    @Test
    @DisplayName("listener: con comprobante ACEPTADO, lineaFacturaRepository.save directo → excepción y BD sin cambios")
    void listener_saveDirecto_rechazado() throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, "ACEPTADO");
        Estado antes = estado(idFactura);
        int idLinea = antes.lineas().get(0).id();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            LineaFactura linea = lineaFacturaRepository.findById(idLinea).orElseThrow();
            linea.setCantidad(7);
            lineaFacturaRepository.saveAndFlush(linea);
        })).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            LineaFactura nueva = new LineaFactura();
            nueva.setFactura(facturaRepository.findById(idFactura).orElseThrow());
            nueva.setProducto(productoRepository.findById(exento.getIdProducto()).orElseThrow());
            nueva.setNumeroLinea(3);
            nueva.setCantidad(1);
            nueva.setPrecioUnitario(new BigDecimal("500.00"));
            nueva.setSubtotal(new BigDecimal("500.00"));
            lineaFacturaRepository.saveAndFlush(nueva);
        })).isInstanceOf(RuntimeException.class);

        assertThat(estado(idFactura)).isEqualTo(antes);
    }

    @Test
    @DisplayName("listener: con comprobante ACEPTADO, lineaFacturaRepository.delete directo → excepción y BD sin cambios")
    void listener_deleteDirecto_rechazado() throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, "ACEPTADO");
        Estado antes = estado(idFactura);
        int idLinea = antes.lineas().get(1).id();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            lineaFacturaRepository.delete(lineaFacturaRepository.findById(idLinea).orElseThrow());
            lineaFacturaRepository.flush();
        })).isInstanceOf(RuntimeException.class);

        assertThat(estado(idFactura)).isEqualTo(antes);
    }

    // ── Fixture ──────────────────────────────────────────────────────────────

    /** Factura en CRC con A (gravado 13 %, 1130) y B (exento, 500) guardadas por la ruta de la UI: total 1630. */
    private int facturaConLineasAyB() throws Exception {
        int idFactura = tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T025 " + UUID.randomUUID());
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T025-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T025");
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
        MvcResult result = guardarLineas(List.of(
                linea(-1, idFactura, gravado13, 1, "1130.00"),
                linea(-2, idFactura, exento, 1, "500.00")));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(totales(idFactura).total()).isEqualByComparingTo("1630.00");
        return idFactura;
    }

    /**
     * Comprobante vigente de la factura en el estado indicado ("SIN_COMPROBANTE" no crea ninguno). Se inserta por
     * SQL para fijar cualquier estado sin pasar por la generación ni por Hacienda.
     */
    private void comprobante(int idFactura, String estado) {
        if ("SIN_COMPROBANTE".equals(estado)) {
            return;
        }
        jdbc.update("INSERT INTO comprobante_electronico (clave_numerica, consecutivo, created_at, updated_at, estado, "
                        + "fecha_emision, fecha_envio, tipo_comprobante, empresa_id, factura_id, intentos_envio, "
                        + "enviado_email, xml_comprobante) "
                        + "VALUES (?, ?, NOW(6), NOW(6), ?, NOW(6), ?, 'FACTURA_ELECTRONICA', ?, ?, 0, b'0', ?)",
                clave(), String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)), estado,
                LocalDateTime.now().minusHours(1), idEmpresa, idFactura, "<FacturaElectronica>T025</FacturaElectronica>");
    }

    private static String clave() {
        StringBuilder clave = new StringBuilder("506");
        while (clave.length() < 50) {
            clave.append(ThreadLocalRandom.current().nextInt(10));
        }
        return clave.toString();
    }

    @SuppressWarnings("deprecation")
    private Producto producto(String descripcion, String precio, boolean gravado) {
        Producto p = new Producto();
        p.setCodigo("T25-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDescripcion(descripcion);
        p.setPrecioInstitucional(new BigDecimal(precio));
        p.setPrecioMayorista(new BigDecimal(precio));
        p.setActive(true);
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(gravado ? new BigDecimal("13") : BigDecimal.ZERO);
        p.setPrecioIncluyeIva(true);
        return productoRepository.save(p);
    }

    private CuentaContable cuenta(String codigo, String nombre, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setNaturaleza(NaturalezaCuenta.fromTipoCuenta(tipo));
        c.setNivel(codigo.split("\\.").length);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }

    /** Línea tal como la arma guardarLineas() en editar-factura.js (ids negativos = filas nuevas). */
    private static Map<String, Object> linea(int idLinea, int idFactura, Producto producto, int cantidad, String precio) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("id_factura", idFactura);
        linea.put("id_linea_factura", idLinea);
        linea.put("numero_linea", 1);
        linea.put("id_producto", producto.getIdProducto());
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", new BigDecimal(precio));
        linea.put("subtotal", new BigDecimal(precio));
        return linea;
    }

    private MvcResult guardarLineas(List<Map<String, Object>> lineas) throws Exception {
        List<Map<String, Object>> numeradas = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            Map<String, Object> l = new LinkedHashMap<>(lineas.get(i));
            l.put("numero_linea", i + 1);
            numeradas.add(l);
        }
        return mockMvc.perform(put("/lineas-factura/actualizar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(numeradas)))
                .andReturn();
    }

    /** Cuerpo de POST /facturas/guardar para editar la factura (entidad completa) cambiando la condición de venta. */
    private Map<String, Object> facturaEditada(int idFactura, String condicionVenta) {
        return jdbc.queryForObject("SELECT numero_factura, serie, id_cliente, total, subtotal, igv, create_date FROM factura "
                + "WHERE id_factura = ?", (rs, i) -> {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("idFactura", idFactura);
            f.put("numeroFactura", rs.getString(1));
            f.put("serie", rs.getString(2));
            f.put("cliente", Map.of("idCliente", rs.getInt(3)));
            f.put("monedaFE", "CRC");
            f.put("tipoCambio", 1);
            f.put("condicionVentaFE", condicionVenta);
            f.put("tipoFactura", "INSTITUCIONAL");
            f.put("total", rs.getBigDecimal(4));
            f.put("subtotal", rs.getBigDecimal(5));
            f.put("igv", rs.getBigDecimal(6));
            // La edición por esta ruta reemplaza la entidad completa: sin createDate la fecha de emisión queda nula
            f.put("createDate", rs.getTimestamp(7).toLocalDateTime().toString());
            return f;
        }, idFactura);
    }

    // ── Lecturas directas de la BD ───────────────────────────────────────────

    record LineaBd(int id, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    record TotalesBd(BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
    }

    record AsientoBd(Long id, BigDecimal total) {
    }

    record Estado(List<LineaBd> lineas, TotalesBd totales, BigDecimal sumaLineas, AsientoBd asientoVigente) {
    }

    private List<LineaBd> lineas(int idFactura) {
        return jdbc.query("SELECT id_linea_factura, cantidad, precio_unitario, subtotal FROM linea_factura "
                        + "WHERE id_factura = ? ORDER BY id_linea_factura",
                (rs, i) -> new LineaBd(rs.getInt(1), rs.getInt(2), rs.getBigDecimal(3), rs.getBigDecimal(4)),
                idFactura);
    }

    private TotalesBd totales(int idFactura) {
        return jdbc.queryForObject("SELECT subtotal, igv, total FROM factura WHERE id_factura = ?",
                (rs, i) -> new TotalesBd(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)),
                idFactura);
    }

    private String condicionVenta(int idFactura) {
        return jdbc.queryForObject("SELECT condicion_venta_fe FROM factura WHERE id_factura = ?", String.class,
                idFactura);
    }

    private int comprobantes(int idFactura) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ?",
                Integer.class, idFactura);
    }

    private AsientoBd asientoVigente(int idFactura) {
        return tx.execute(s -> {
            List<AsientoContable> vigentes = asientoRepository.findAsientosVentaVigentes((long) idFactura);
            assertThat(vigentes).hasSizeLessThanOrEqualTo(1);
            if (vigentes.isEmpty()) {
                return null;
            }
            AsientoContable asiento = vigentes.get(0);
            return new AsientoBd(asiento.getIdAsiento(), asiento.getTotalDebe().stripTrailingZeros());
        });
    }

    private Estado estado(int idFactura) {
        BigDecimal suma = jdbc.queryForObject(
                "SELECT COALESCE(SUM(subtotal), 0) FROM linea_factura WHERE id_factura = ?", BigDecimal.class,
                idFactura);
        return new Estado(lineas(idFactura), totales(idFactura), suma, asientoVigente(idFactura));
    }
}
