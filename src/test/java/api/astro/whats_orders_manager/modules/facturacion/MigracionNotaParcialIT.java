package api.astro.whats_orders_manager.modules.facturacion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-047 (criterios 22, 25, 26, 28 y 29): el SQL real de V46 sobre MySQL 8.0.32, en tablas mínimas con las columnas que
 * usa. Cubre el árbol 4.1 → 4.1.02 → {4.1.02.001, 4.1.02.002}, los parámetros contables nuevos, la idempotencia frente a
 * códigos existentes, el renombre de las cuentas de diferencial (sin perder movimientos) y las columnas de la factura.
 */
@Testcontainers
@DisplayName("T-047 — migración V46: nota parcial, cuentas 4.1.02 y renombre de diferencial")
class MigracionNotaParcialIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t047")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t047-it-mysql-migraciones"));

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE factura (id_factura INT NOT NULL PRIMARY KEY, total DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE cuentas_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "codigo VARCHAR(20) NOT NULL UNIQUE, nombre VARCHAR(200) NOT NULL, tipo VARCHAR(20) NOT NULL, "
                + "naturaleza VARCHAR(20) NOT NULL, nivel INT NOT NULL, activa BIT NOT NULL, "
                + "acepta_movimientos BIT NOT NULL, cuenta_padre_id BIGINT NULL, descripcion VARCHAR(500), "
                + "creado_por INT NULL, fecha_creacion DATETIME NULL, modificado_por INT NULL, "
                + "fecha_modificacion DATETIME NULL)");
        jdbc.execute("CREATE TABLE parametros_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "clave VARCHAR(100) NOT NULL UNIQUE, descripcion VARCHAR(255), cuenta_contable_id BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE detalles_asiento (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "cuenta_id BIGINT NOT NULL, debe DECIMAL(15,2), haber DECIMAL(15,2))");

        // Plan vigente (V29): 4.1 y 4.1.01 con padre, hojas de ventas y de diferencial con movimientos
        cuenta(1, "4.1", "Ingresos Operativos", "ACREEDORA", 2, false, null);
        cuenta(2, "4.1.01", "Ventas", "ACREEDORA", 3, false, 1L);
        cuenta(3, "4.1.01.001", "Ingresos por Ventas", "ACREEDORA", 4, true, 2L);
        cuenta(4, "4.2.01.001", "Diferencial Cambiario Ganado", "ACREEDORA", 4, true, null);
        cuenta(5, "5.2.01.001", "Diferencial Cambiario Perdido", "DEUDORA", 4, true, null);
        // Idempotencia: 4.1.02.001 ya existe con otro nombre (no se toca ni se duplica)
        cuenta(6, "4.1.02.001", "Nombre existente", "DEUDORA", 4, true, null);
        jdbc.update("INSERT INTO detalles_asiento (cuenta_id, debe, haber) VALUES (4, NULL, 25.00), (5, 10.00, NULL)");
        jdbc.update("INSERT INTO factura VALUES (1, 100.00)");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V46__nota_credito_parcial.sql")).execute(ds);
    }

    private static void cuenta(long id, String codigo, String nombre, String naturaleza, int nivel, boolean acepta,
                               Long padre) {
        jdbc.update("INSERT INTO cuentas_contables (id, codigo, nombre, tipo, naturaleza, nivel, activa, "
                        + "acepta_movimientos, cuenta_padre_id) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)", id, codigo, nombre,
                codigo.startsWith("5") ? "EGRESO" : "INGRESO", naturaleza, nivel, acepta, padre);
    }

    private static Map<String, Object> cuenta(String codigo) {
        return jdbc.queryForMap("SELECT * FROM cuentas_contables WHERE codigo = ?", codigo);
    }

    @Test
    @DisplayName("el árbol queda 4.1 → 4.1.02 (grupo) → 4.1.02.001 y 4.1.02.002 (hojas INGRESO/DEUDORA que aceptan movimientos)")
    void arbol() {
        Map<String, Object> grupo = cuenta("4.1.02");
        assertThat(grupo.get("nombre")).isEqualTo("Devoluciones y descuentos sobre ventas");
        assertThat(grupo.get("cuenta_padre_id")).isEqualTo(1L);
        assertThat(((Number) grupo.get("nivel")).intValue()).isEqualTo(3);
        assertThat(grupo.get("acepta_movimientos")).isIn(false, 0, Boolean.FALSE);
        assertThat(grupo.get("tipo")).isEqualTo("INGRESO");

        Map<String, Object> anulaciones = cuenta("4.1.02.002");
        assertThat(anulaciones.get("nombre")).isEqualTo("Anulaciones, descuentos y rebajas sobre ventas");
        assertThat(anulaciones.get("cuenta_padre_id")).isEqualTo(grupo.get("id"));
        assertThat(anulaciones.get("tipo")).isEqualTo("INGRESO");
        assertThat(anulaciones.get("naturaleza")).isEqualTo("DEUDORA");
        assertThat(((Number) anulaciones.get("nivel")).intValue()).isEqualTo(4);
        assertThat(anulaciones.get("acepta_movimientos")).isIn(true, 1, Boolean.TRUE);

        List<String> hijas = jdbc.queryForList("SELECT codigo FROM cuentas_contables WHERE cuenta_padre_id = ? "
                + "ORDER BY codigo", String.class, grupo.get("id"));
        assertThat(hijas).containsExactly("4.1.02.002");
    }

    @Test
    @DisplayName("idempotente frente a códigos existentes: 4.1.02.001 preexistente no se modifica ni se duplica")
    void idempotente() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_contables WHERE codigo = '4.1.02.001'",
                Integer.class)).isEqualTo(1);
        assertThat(cuenta("4.1.02.001").get("nombre")).isEqualTo("Nombre existente");
    }

    @Test
    @DisplayName("no toca otras cuentas: 4.1, 4.1.01 y 4.1.01.001 iguales; solo se agregan 4.1.02 y 4.1.02.002")
    void noTocaOtrasCuentas() {
        assertThat(cuenta("4.1").get("nombre")).isEqualTo("Ingresos Operativos");
        assertThat(cuenta("4.1.01").get("nombre")).isEqualTo("Ventas");
        assertThat(cuenta("4.1.01.001").get("nombre")).isEqualTo("Ingresos por Ventas");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_contables", Integer.class)).isEqualTo(8);
    }

    @Test
    @DisplayName("dos parámetros contables explícitos apuntan a 4.1.02.001 (devoluciones) y 4.1.02.002 (anulaciones y descuentos)")
    void parametros() {
        assertThat(jdbc.queryForObject("SELECT c.codigo FROM parametros_contables p JOIN cuentas_contables c "
                + "ON c.id = p.cuenta_contable_id WHERE p.clave = 'NOTA_CREDITO_DEVOLUCIONES'", String.class))
                .isEqualTo("4.1.02.001");
        assertThat(jdbc.queryForObject("SELECT c.codigo FROM parametros_contables p JOIN cuentas_contables c "
                + "ON c.id = p.cuenta_contable_id WHERE p.clave = 'NOTA_CREDITO_ANULACIONES_DESCUENTOS'", String.class))
                .isEqualTo("4.1.02.002");
    }

    @Test
    @DisplayName("las cuentas de diferencial cambiario se renombran «– realizado» sin cambiar id, código ni movimientos; no se crean las «no realizado»")
    void renombreDeDiferencial() {
        Map<String, Object> ganado = cuenta("4.2.01.001");
        assertThat(ganado.get("nombre")).isEqualTo("Diferencial Cambiario Ganado – realizado");
        assertThat(ganado.get("id")).isEqualTo(4L);
        assertThat(cuenta("5.2.01.001").get("nombre")).isEqualTo("Diferencial Cambiario Perdido – realizado");
        assertThat(cuenta("5.2.01.001").get("id")).isEqualTo(5L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM detalles_asiento WHERE cuenta_id IN (4, 5)",
                Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_contables WHERE nombre LIKE '%no realizado%'",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("factura: monto_acreditado = 0 y acreditada_totalmente = false para las facturas existentes")
    void columnasDeLaFactura() {
        Map<String, Object> f = jdbc.queryForMap("SELECT monto_acreditado, acreditada_totalmente FROM factura "
                + "WHERE id_factura = 1");
        assertThat(((java.math.BigDecimal) f.get("monto_acreditado"))).isEqualByComparingTo("0");
        assertThat(f.get("acreditada_totalmente")).isIn(false, 0, Boolean.FALSE);
    }
}
