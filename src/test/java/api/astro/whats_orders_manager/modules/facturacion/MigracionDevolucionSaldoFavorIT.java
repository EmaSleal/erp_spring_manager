package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-049 (criterios 1 y 2): el SQL real de V49 sobre MySQL 8.0.32, en tablas mínimas con las columnas que usa. Cubre la
 * reserva con su CHECK (saldo vivo − reservado nunca negativo), la tabla de devoluciones y sus restricciones y el permiso
 * SALDO_FAVOR_DEVOLUCION_APROBAR: creado, asignado solo a ADMIN e idempotente.
 */
@Testcontainers
@DisplayName("T-049 — migración V49: reserva, devoluciones de saldo a favor y permiso de aprobación")
class MigracionDevolucionSaldoFavorIT {

    private static final String CODIGO = "SALDO_FAVOR_DEVOLUCION_APROBAR";
    private static final String RECURSO = "db/migration/V49__devolucion_saldo_favor.sql";

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t049", "t049-it-mysql-migraciones");

    static JdbcTemplate jdbc;
    static DriverManagerDataSource ds;

    @BeforeAll
    static void migrar() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE saldos_favor_cliente (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "saldo_vivo DECIMAL(12,2) NOT NULL, CONSTRAINT ck_saldo_favor_saldo_vivo CHECK (saldo_vivo >= 0))");
        jdbc.execute("CREATE TABLE cuenta_bancaria (id_cuenta_bancaria INT NOT NULL AUTO_INCREMENT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE asientos_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE rol (id_rol INT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(50) NOT NULL UNIQUE)");
        jdbc.execute("CREATE TABLE permiso (id_permiso INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "codigo VARCHAR(100) NOT NULL UNIQUE, nombre VARCHAR(100) NOT NULL, descripcion VARCHAR(255), "
                + "categoria VARCHAR(50), es_critico BIT NOT NULL, activo BIT NOT NULL, create_by INT NULL, "
                + "create_date DATETIME NULL, update_by INT NULL, update_date DATETIME NULL)");
        jdbc.execute("CREATE TABLE rol_permiso (id_rol INT NOT NULL, id_permiso INT NOT NULL, "
                + "PRIMARY KEY (id_rol, id_permiso))");
        for (String rol : List.of("ADMIN", "GERENTE", "VENDEDOR", "USER", "VISUALIZADOR", "CLIENTE")) {
            jdbc.update("INSERT INTO rol (codigo) VALUES (?)", rol);
        }
        // saldos existentes: quedan con 0 reservado
        jdbc.update("INSERT INTO saldos_favor_cliente (saldo_vivo) VALUES (500.00)");
        jdbc.update("INSERT INTO saldos_favor_cliente (saldo_vivo) VALUES (100.00)");
        // idempotencia: el permiso ya existe (alta manual) con otro nombre
        jdbc.update("INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) "
                + "VALUES (?, 'Nombre existente', 'x', 'Pagos', 1, 1)", CODIGO);

        new ResourceDatabasePopulator(new ClassPathResource(RECURSO)).execute(ds);
    }

    @Test
    @DisplayName("el permiso existe una sola vez y conserva lo que ya tenía; solo ADMIN lo recibe (no hay rol de contabilidad)")
    void permisoSoloParaAdmin() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = ?", Integer.class, CODIGO)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT nombre FROM permiso WHERE codigo = ?", String.class, CODIGO))
                .isEqualTo("Nombre existente");
        List<String> roles = jdbc.queryForList("SELECT r.codigo FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol "
                + "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE p.codigo = ?", String.class, CODIGO);
        assertThat(roles).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("idempotente: repetir las sentencias del permiso no duplica el permiso ni la asignación")
    void idempotente() throws Exception {
        String sql = new String(new ClassPathResource(RECURSO).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String soloPermiso = sql.substring(sql.indexOf("INSERT IGNORE INTO permiso"));
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new org.springframework.core.io.ByteArrayResource(soloPermiso.getBytes(StandardCharsets.UTF_8)));
        populator.execute(ds);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = ?", Integer.class, CODIGO)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rol_permiso rp JOIN permiso p ON p.id_permiso = rp.id_permiso "
                + "WHERE p.codigo = ?", Integer.class, CODIGO)).isEqualTo(1);
    }

    @Test
    @DisplayName("monto_reservado: 0 en los saldos existentes y CHECK saldo_vivo − reservado >= 0 (y reservado >= 0)")
    void reservaConCheck() {
        assertThat(jdbc.queryForList("SELECT monto_reservado FROM saldos_favor_cliente", BigDecimal.class))
                .allSatisfy(v -> assertThat(v).isEqualByComparingTo("0"));
        jdbc.update("UPDATE saldos_favor_cliente SET monto_reservado = 500.00 WHERE saldo_vivo = 500.00");
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "UPDATE saldos_favor_cliente SET monto_reservado = 500.01 WHERE saldo_vivo = 500.00"));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "UPDATE saldos_favor_cliente SET monto_reservado = -0.01 WHERE saldo_vivo = 100.00"));
        // bajar el saldo vivo por debajo de lo reservado también viola el CHECK
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "UPDATE saldos_favor_cliente SET saldo_vivo = 400.00 WHERE saldo_vivo = 500.00"));
        jdbc.update("UPDATE saldos_favor_cliente SET monto_reservado = 0 WHERE saldo_vivo = 500.00");
    }

    @Test
    @DisplayName("devoluciones_saldo_favor: columnas de ADR-058, monto > 0, FK al saldo, a la cuenta bancaria y al asiento")
    void tablaDevoluciones() {
        long idSaldo = jdbc.queryForObject("SELECT id FROM saldos_favor_cliente WHERE saldo_vivo = 100.00", Long.class);
        jdbc.update("INSERT INTO cuenta_bancaria VALUES (1)");
        jdbc.update("INSERT INTO asientos_contables VALUES (1)");
        String insert = "INSERT INTO devoluciones_saldo_favor (saldo_favor_id, monto, estado, medio_solicitud, "
                + "fecha_solicitud, iban, nombre_titular, identificacion_titular, solicitado_por, fecha_creacion, "
                + "cuenta_bancaria_id, asiento_id) VALUES (?, ?, 'SOLICITADA', 'CORREO', '2026-11-20', 'CR0000', "
                + "'Titular', '109990999', 1, NOW(), ?, ?)";
        jdbc.update(insert, idSaldo, new BigDecimal("10.00"), 1, 1);
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(insert, idSaldo, BigDecimal.ZERO, 1, 1));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert, 999L, new BigDecimal("1.00"), 1, 1));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert, idSaldo, new BigDecimal("1.00"), 99, 1));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert, idSaldo, new BigDecimal("1.00"), 1, 99));
        Map<String, Object> d = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor");
        assertThat(d).containsKeys("estado", "medio_solicitud", "fecha_solicitud", "iban", "nombre_titular",
                "identificacion_titular", "solicitado_por", "fecha_creacion", "aprobado_por", "fecha_aprobacion",
                "verifico_cuenta", "fecha_verificacion", "rechazado_por", "fecha_rechazo", "anulado_por",
                "fecha_anulacion", "razon", "pagado_por", "fecha_pago", "cuenta_bancaria_id", "numero_comprobante",
                "fecha_comprobante", "tipo_cambio_origen", "tc_dia_fecha", "tc_dia_valor", "tc_dia_tipo",
                "tc_dia_fuente", "tc_dia_origen", "diferencial_saldo", "diferencial_neto", "asiento_id");
        assertThat(d.get("verifico_cuenta")).isIn(false, 0, Boolean.FALSE);
    }
}
