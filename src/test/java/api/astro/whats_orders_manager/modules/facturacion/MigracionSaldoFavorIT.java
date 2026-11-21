package api.astro.whats_orders_manager.modules.facturacion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-048 (criterios 1, 2, 3 y 4): el SQL real de V48 sobre MySQL 8.0.32, en tablas mínimas con las columnas que usa.
 * Cubre el grupo 2.1.05 bajo 2.1 sin movimientos, las hojas 2.1.05.001 (CRC) y 2.1.05.002 (USD), la idempotencia, que no
 * toca otras cuentas, los dos parámetros contables y las tablas nuevas con su CHECK de saldo vivo no negativo.
 */
@Testcontainers
@DisplayName("T-048 — migración V48: cuentas 2.1.05, parámetros y tablas de saldos a favor")
class MigracionSaldoFavorIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t048")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t048-it-mysql-migraciones"));

    static JdbcTemplate jdbc;
    static DriverManagerDataSource ds;

    @BeforeAll
    static void migrar() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE cliente (id_cliente INT NOT NULL AUTO_INCREMENT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE factura (id_factura INT NOT NULL PRIMARY KEY, total DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE nota_factura (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "factura_id INT NOT NULL, total DECIMAL(12,2) NOT NULL)");
        jdbc.execute("CREATE TABLE pagos (id_pago BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, monto DECIMAL(12,2))");
        jdbc.execute("CREATE TABLE asientos_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE cuentas_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "codigo VARCHAR(20) NOT NULL UNIQUE, nombre VARCHAR(200) NOT NULL, tipo VARCHAR(20) NOT NULL, "
                + "naturaleza VARCHAR(20) NOT NULL, nivel INT NOT NULL, activa BIT NOT NULL, "
                + "acepta_movimientos BIT NOT NULL, cuenta_padre_id BIGINT NULL, descripcion VARCHAR(500), "
                + "creado_por INT NULL, fecha_creacion DATETIME NULL, modificado_por INT NULL, "
                + "fecha_modificacion DATETIME NULL)");
        jdbc.execute("CREATE TABLE parametros_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "clave VARCHAR(100) NOT NULL UNIQUE, descripcion VARCHAR(255), cuenta_contable_id BIGINT NOT NULL)");

        // Plan vigente: 2 → 2.1 → 2.1.02 y 2.1.03 (otros grupos de pasivo que no se tocan)
        cuenta(1, "2", "Pasivo", 1, false, null);
        cuenta(2, "2.1", "Pasivo Corriente", 2, false, 1L);
        cuenta(3, "2.1.03", "Anticipos", 3, false, 2L);
        cuenta(4, "2.1.03.001", "Anticipos de Clientes", 4, true, 3L);
        // Idempotencia: 2.1.05.001 ya existe con otro nombre (no se toca ni se duplica)
        cuenta(5, "2.1.05.001", "Nombre existente", 4, true, null);
        jdbc.update("INSERT INTO cliente VALUES (1)");
        jdbc.update("INSERT INTO factura VALUES (1, 100.00)");
        jdbc.update("INSERT INTO nota_factura (id, factura_id, total) VALUES (1, 1, 50.00)");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V48__saldo_a_favor_clientes.sql")).execute(ds);
    }

    private static void cuenta(long id, String codigo, String nombre, int nivel, boolean acepta, Long padre) {
        jdbc.update("INSERT INTO cuentas_contables (id, codigo, nombre, tipo, naturaleza, nivel, activa, "
                + "acepta_movimientos, cuenta_padre_id) VALUES (?, ?, ?, 'PASIVO', 'ACREEDORA', ?, 1, ?, ?)", id, codigo,
                nombre, nivel, acepta, padre);
    }

    private static Map<String, Object> cuenta(String codigo) {
        return jdbc.queryForMap("SELECT * FROM cuentas_contables WHERE codigo = ?", codigo);
    }

    @Test
    @DisplayName("el árbol queda 2.1 → 2.1.05 (grupo sin movimientos, padre 2.1 y no NULL) → 2.1.05.002 (hoja PASIVO/ACREEDORA, nivel 4)")
    void arbol() {
        Map<String, Object> grupo = cuenta("2.1.05");
        assertThat(grupo.get("nombre")).isEqualTo("Saldos a favor de clientes");
        assertThat(grupo.get("cuenta_padre_id")).isEqualTo(2L);
        assertThat(((Number) grupo.get("nivel")).intValue()).isEqualTo(3);
        assertThat(grupo.get("acepta_movimientos")).isIn(false, 0, Boolean.FALSE);
        assertThat(grupo.get("tipo")).isEqualTo("PASIVO");
        assertThat(grupo.get("naturaleza")).isEqualTo("ACREEDORA");

        Map<String, Object> usd = cuenta("2.1.05.002");
        assertThat(usd.get("nombre")).isEqualTo("Saldos a favor de clientes – USD");
        assertThat(usd.get("cuenta_padre_id")).isEqualTo(grupo.get("id"));
        assertThat(usd.get("tipo")).isEqualTo("PASIVO");
        assertThat(usd.get("naturaleza")).isEqualTo("ACREEDORA");
        assertThat(((Number) usd.get("nivel")).intValue()).isEqualTo(4);
        assertThat(usd.get("acepta_movimientos")).isIn(true, 1, Boolean.TRUE);
        assertThat(usd.get("activa")).isIn(true, 1, Boolean.TRUE);
    }

    @Test
    @DisplayName("idempotente: 2.1.05.001 preexistente no se modifica ni se duplica")
    void idempotente() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_contables WHERE codigo = '2.1.05.001'",
                Integer.class)).isEqualTo(1);
        assertThat(cuenta("2.1.05.001").get("nombre")).isEqualTo("Nombre existente");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_contables WHERE codigo = '2.1.05'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("no toca otras cuentas: solo se agregan 2.1.05 y 2.1.05.002")
    void noTocaOtrasCuentas() {
        assertThat(cuenta("2.1").get("nombre")).isEqualTo("Pasivo Corriente");
        assertThat(cuenta("2.1.03").get("nombre")).isEqualTo("Anticipos");
        assertThat(cuenta("2.1.03.001").get("nombre")).isEqualTo("Anticipos de Clientes");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_contables", Integer.class)).isEqualTo(7);
    }

    @Test
    @DisplayName("dos parámetros contables explícitos, uno por moneda, apuntan a las hojas 2.1.05.001 y 2.1.05.002")
    void parametros() {
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT p.clave, c.codigo FROM parametros_contables p "
                + "JOIN cuentas_contables c ON c.id = p.cuenta_contable_id WHERE p.clave LIKE 'SALDO_FAVOR_CLIENTES_%' "
                + "ORDER BY p.clave");
        assertThat(filas).hasSize(2);
        assertThat(filas.get(0)).containsEntry("clave", "SALDO_FAVOR_CLIENTES_CRC").containsEntry("codigo", "2.1.05.001");
        assertThat(filas.get(1)).containsEntry("clave", "SALDO_FAVOR_CLIENTES_USD").containsEntry("codigo", "2.1.05.002");
    }

    @Test
    @DisplayName("tablas nuevas: saldo vivo con CHECK >= 0, FKs y columnas del reparto en la nota")
    void tablasNuevas() {
        jdbc.update("INSERT INTO saldos_favor_cliente (cliente_id, moneda, monto_original, tipo_cambio_origen, "
                + "fecha_origen, saldo_vivo, nota_factura_id, clave_nota, factura_origen_id, estado, fecha_creacion) "
                + "VALUES (1, 'CRC', 50.00, 1, '2026-11-20', 50.00, 1, ?, 1, 'VIGENTE', NOW())", "5".repeat(50));
        // MySQL 8.0.32 informa el CHECK violado (error 3819) como SQLException genérica
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "UPDATE saldos_favor_cliente SET saldo_vivo = -0.01 WHERE nota_factura_id = 1"));
        // un saldo por nota
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO saldos_favor_cliente (cliente_id, moneda, monto_original, tipo_cambio_origen, fecha_origen, "
                        + "saldo_vivo, nota_factura_id, clave_nota, factura_origen_id, estado, fecha_creacion) "
                        + "VALUES (1, 'CRC', 1.00, 1, '2026-11-20', 1.00, 1, ?, 1, 'VIGENTE', NOW())", "5".repeat(50)));
        long idSaldo = jdbc.queryForObject("SELECT id FROM saldos_favor_cliente", Long.class);
        jdbc.update("INSERT INTO pagos (id_pago, monto) VALUES (1, 10.00)");
        jdbc.update("INSERT INTO asientos_contables (id) VALUES (1)");
        jdbc.update("INSERT INTO aplicaciones_saldo_favor (saldo_favor_id, factura_id, pago_id, monto, "
                + "tipo_cambio_origen, tipo_cambio_factura, diferencial_saldo, diferencial_cxc, diferencial_neto, "
                + "asiento_id, estado, usuario_id, fecha_aplicacion) VALUES (?, 1, 1, 10.00, 1, 1, 0, 0, 0, 1, "
                + "'VIGENTE', 1, NOW())", idSaldo);
        // la aplicación exige su saldo
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO aplicaciones_saldo_favor (saldo_favor_id, factura_id, pago_id, monto, tipo_cambio_origen, "
                        + "tipo_cambio_factura, diferencial_saldo, diferencial_cxc, diferencial_neto, asiento_id, estado, "
                        + "usuario_id, fecha_aplicacion) VALUES (999, 1, 1, 1, 1, 1, 0, 0, 0, 1, 'VIGENTE', 1, NOW())"));
        // columnas de reversión y de TC del día
        Map<String, Object> a = jdbc.queryForMap("SELECT * FROM aplicaciones_saldo_favor");
        assertThat(a).containsKeys("tc_dia_fecha", "tc_dia_valor", "tc_dia_tipo", "tc_dia_fuente", "tc_dia_origen",
                "razon_reversion", "reversado_por", "fecha_reversion", "asiento_reversion_id");
        // nota_factura: reparto, nulo hasta APLICADA
        jdbc.update("UPDATE nota_factura SET monto_credito_cxc = 10.00, monto_saldo_favor = 40.00 WHERE id = 1");
        assertThat(jdbc.queryForObject("SELECT monto_saldo_favor FROM nota_factura WHERE id = 1",
                java.math.BigDecimal.class)).isEqualByComparingTo("40.00");
    }
}
