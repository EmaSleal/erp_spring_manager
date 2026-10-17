package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-024 (ADR-028 etapa 1, P-046) — guardar las líneas de una factura sobre MySQL real.
 *
 * El cuerpo de PUT /lineas-factura/actualizar es el conjunto completo de líneas de la factura: las líneas en BD
 * son exactamente las recibidas, los montos se calculan en el servidor (cantidad × precio, IVA incluido, ADR-009)
 * y factura.subtotal/igv/total, el asiento de venta y las líneas del XML de FE salen del mismo conjunto.
 *
 * Base: MySQL 8 en Testcontainers (contenedor t024-it-mysql, datos en tmpfs, sin volúmenes), esquema creado por
 * Hibernate desde las entidades y las rutinas de database/routines cargadas al iniciar el contenedor, igual que
 * en producción. Ninguna prueba toca un MySQL local ni de producción, ni llama a Hacienda.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-024 — sincronización de líneas de factura sobre MySQL (Testcontainers)")
class LineaFacturaSincronizacionIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t024")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t024-it-mysql"))
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

    /** Administrador de bootstrap que DataInitializer crea en la base de pruebas (src/test/resources/application.yml). */
    private static final String USUARIO = "admin.bootstrap@example.invalid";

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

    private TransactionTemplate tx;
    private Producto gravado13;
    private Producto exento;

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
            gravado13 = producto("Producto A gravado 13 %", "1130.00", true);
            exento = producto("Producto B exento", "500.00", false);
        });
    }

    // ── Criterio: línea quitada ──────────────────────────────────────────────

    @Test
    @DisplayName("quitar la línea B y guardar solo A → 1 línea en BD; 1000 + 130 = 1130; asiento vigente 1130")
    void lineaQuitada_quedaSoloLaRecibida() throws Exception {
        int idFactura = facturaConLineasAyB();
        LineaBd lineaA = lineas(idFactura).get(0);

        guardar(List.of(linea(lineaA.id(), idFactura, gravado13, 1, "1130.00", "1130.00")))
                .andExpect(status().isOk());

        assertThat(lineas(idFactura)).hasSize(1);
        TotalesBd totales = totales(idFactura);
        assertThat(totales.total()).isEqualByComparingTo("1130.00");
        assertThat(totales.subtotal()).isEqualByComparingTo("1000.00");
        assertThat(totales.igv()).isEqualByComparingTo("130.00");
        assertThat(sumaSubtotalesBd(idFactura)).isEqualByComparingTo(totales.total());
        assertThat(totalAsientoVentaVigente(idFactura)).isEqualByComparingTo("1130.00");
    }

    @Test
    @DisplayName("consistencia con FE: tras quitar una línea, las líneas que lee el XML suman factura.total")
    void lineaQuitada_lineasDelXmlSumanElTotal() throws Exception {
        int idFactura = facturaConLineasAyB();
        LineaBd lineaA = lineas(idFactura).get(0);

        guardar(List.of(linea(lineaA.id(), idFactura, gravado13, 1, "1130.00", "1130.00")))
                .andExpect(status().isOk());

        BigDecimal total = totales(idFactura).total();
        tx.executeWithoutResult(s -> {
            // Método vigente del repositorio para cargar las líneas como entidades (XML de comprobante)
            BigDecimal sumaRepositorio = lineaFacturaRepository.findEntitiesByFacturaId(idFactura).stream()
                    .map(LineaFactura::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sumaRepositorio).isEqualByComparingTo(total);
            // XmlGeneratorServiceImpl recorre factura.getLineas()
            BigDecimal sumaFactura = facturaRepository.findById(idFactura).orElseThrow().getLineas().stream()
                    .map(LineaFactura::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sumaFactura).isEqualByComparingTo(total);
        });
    }

    // ── Criterio: montos del navegador ignorados ─────────────────────────────

    @Test
    @DisplayName("cantidad 2 × precio 1130 con subtotal 1 enviado → persiste 2260.00 y la factura totaliza 2260.00")
    void subtotalDelNavegador_seIgnora() throws Exception {
        int idFactura = facturaVacia();

        guardar(List.of(linea(-1, idFactura, gravado13, 2, "1130.00", "1.00")))
                .andExpect(status().isOk());

        List<LineaBd> lineas = lineas(idFactura);
        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0).subtotal()).isEqualByComparingTo("2260.00");
        TotalesBd totales = totales(idFactura);
        assertThat(totales.total()).isEqualByComparingTo("2260.00");
        assertThat(totales.subtotal()).isEqualByComparingTo("2000.00");
        assertThat(totales.igv()).isEqualByComparingTo("260.00");
    }

    // ── Criterio: línea ajena ────────────────────────────────────────────────

    @Test
    @DisplayName("id de una línea de otra factura → 400 y ninguna de las dos facturas cambia")
    void lineaDeOtraFactura_rechazada() throws Exception {
        int idFactura1 = facturaVacia();
        guardar(List.of(linea(-1, idFactura1, gravado13, 1, "1130.00", "1130.00"))).andExpect(status().isOk());
        int idFactura2 = facturaVacia();
        guardar(List.of(linea(-1, idFactura2, exento, 1, "500.00", "500.00"))).andExpect(status().isOk());
        Estado antes1 = estado(idFactura1);
        Estado antes2 = estado(idFactura2);
        int idLineaAjena = antes2.lineas().get(0).id();

        guardar(List.of(linea(idLineaAjena, idFactura1, exento, 3, "500.00", "1500.00")))
                .andExpect(status().isBadRequest());

        assertThat(estado(idFactura1)).isEqualTo(antes1);
        assertThat(estado(idFactura2)).isEqualTo(antes2);
    }

    // ── Criterio: validaciones ───────────────────────────────────────────────

    @ParameterizedTest(name = "cantidad {0}, precio {1} → 400 sin cambios")
    @CsvSource({"0, 1130.00", "-1, 1130.00", "1, -1.00"})
    @DisplayName("cantidad 0 o negativa, o precio negativo → 400 sin cambios")
    void montosInvalidos_rechazados(int cantidad, String precio) throws Exception {
        int idFactura = facturaConLineasAyB();
        Estado antes = estado(idFactura);
        LineaBd lineaA = antes.lineas().get(0);
        LineaBd lineaB = antes.lineas().get(1);

        guardar(List.of(
                linea(lineaA.id(), idFactura, gravado13, cantidad, precio, "1130.00"),
                linea(lineaB.id(), idFactura, exento, 1, "500.00", "500.00")))
                .andExpect(status().isBadRequest());

        assertThat(estado(idFactura)).isEqualTo(antes);
    }

    // ── Fixture ──────────────────────────────────────────────────────────────

    /** Factura en CRC sin líneas, como la deja POST /facturas/guardar en el flujo de la UI. */
    private int facturaVacia() {
        return tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T024 " + UUID.randomUUID());
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T024-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
    }

    /** Factura con A (gravado 13 %, 1130) y B (exento, 500) guardadas por la ruta de la UI: total 1630. */
    private int facturaConLineasAyB() throws Exception {
        int idFactura = facturaVacia();
        guardar(List.of(
                linea(-1, idFactura, gravado13, 1, "1130.00", "1130.00"),
                linea(-2, idFactura, exento, 1, "500.00", "500.00")))
                .andExpect(status().isOk());
        assertThat(lineas(idFactura)).hasSize(2);
        assertThat(totales(idFactura).total()).isEqualByComparingTo("1630.00");
        return idFactura;
    }

    @SuppressWarnings("deprecation")
    private Producto producto(String descripcion, String precio, boolean gravado) {
        Producto p = new Producto();
        p.setCodigo("T24-" + UUID.randomUUID().toString().substring(0, 8));
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
    private static Map<String, Object> linea(int idLinea, int idFactura, Producto producto, int cantidad,
                                             String precio, String subtotal) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("id_factura", idFactura);
        linea.put("id_linea_factura", idLinea);
        linea.put("numero_linea", 1);
        linea.put("id_producto", producto.getIdProducto());
        linea.put("descripcion", null);
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", new BigDecimal(precio));
        linea.put("subtotal", new BigDecimal(subtotal));
        linea.put("create_by", null);
        linea.put("update_by", null);
        linea.put("create_date", null);
        linea.put("update_date", null);
        return linea;
    }

    private ResultActions guardar(List<Map<String, Object>> lineas) throws Exception {
        List<Map<String, Object>> numeradas = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            Map<String, Object> l = new LinkedHashMap<>(lineas.get(i));
            l.put("numero_linea", i + 1);
            numeradas.add(l);
        }
        return mockMvc.perform(put("/lineas-factura/actualizar")
                .with(user(USUARIO).roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(numeradas)));
    }

    // ── Lecturas directas de la BD ───────────────────────────────────────────

    record LineaBd(int id, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    record TotalesBd(BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
    }

    record Estado(List<LineaBd> lineas, TotalesBd totales, BigDecimal asientoVigente) {
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

    private BigDecimal sumaSubtotalesBd(int idFactura) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(subtotal), 0) FROM linea_factura WHERE id_factura = ?",
                BigDecimal.class, idFactura);
    }

    /** Total del asiento de venta vigente (debe = haber); null si no hay. */
    private BigDecimal totalAsientoVentaVigente(int idFactura) {
        return tx.execute(s -> {
            List<AsientoContable> vigentes = asientoRepository.findAsientosVentaVigentes((long) idFactura);
            assertThat(vigentes).hasSizeLessThanOrEqualTo(1);
            if (vigentes.isEmpty()) {
                return null;
            }
            AsientoContable asiento = vigentes.get(0);
            assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
            return asiento.getTotalDebe().stripTrailingZeros();
        });
    }

    private Estado estado(int idFactura) {
        return new Estado(lineas(idFactura), totales(idFactura), totalAsientoVentaVigente(idFactura));
    }
}
