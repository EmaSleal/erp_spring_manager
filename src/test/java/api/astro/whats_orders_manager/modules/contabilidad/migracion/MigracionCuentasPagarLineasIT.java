package api.astro.whats_orders_manager.modules.contabilidad.migracion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-054 (ADR-067): el SQL real de V55 sobre MySQL 8.0.32, en tablas mínimas con el estado que deja V53. Cubre la
 * migración del desglose de IVA a líneas de la CxP (una línea por fila, cuenta de la CxP o, en su defecto, la de
 * CPP_CUENTA_COMPRAS, tratamiento PLENO), la baja de cuenta_pagar_iva, la tabla de prorrata, las columnas de aceptación
 * y el permiso PRORRATA_IVA_GESTIONAR (idempotente).
 */
@Testcontainers
@DisplayName("T-054 — migración V55: líneas de CxP, prorrata, aceptación del comprobante y permiso")
class MigracionCuentasPagarLineasIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t054m", "t054-it-mysql-lineas");

    static final String ARCHIVO = "db/migration/V55__cuentas_pagar_lineas_prorrata_aceptacion.sql";
    static JdbcTemplate jdbc;
    static DriverManagerDataSource ds;

    @BeforeAll
    static void migrar() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE cuentas_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(20) NOT NULL, "
                + "nombre VARCHAR(200) NOT NULL)");
        jdbc.execute("CREATE TABLE parametros_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "clave VARCHAR(100) NOT NULL UNIQUE, descripcion VARCHAR(300), cuenta_contable_id BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE cuentas_por_pagar (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, numero VARCHAR(20) NOT NULL, "
                + "descripcion VARCHAR(300) NOT NULL, monto DECIMAL(19,2) NOT NULL, cuenta_gasto_id BIGINT NULL)");
        jdbc.execute("CREATE TABLE cuenta_pagar_iva (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, cuenta_por_pagar_id BIGINT NOT NULL, "
                + "codigo_tarifa VARCHAR(2) NULL, base DECIMAL(15,2) NOT NULL, iva DECIMAL(15,2) NOT NULL)");
        jdbc.execute("CREATE TABLE permiso (id_permiso INT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100) NOT NULL UNIQUE, "
                + "nombre VARCHAR(150), descripcion VARCHAR(300), categoria VARCHAR(60), es_critico BOOLEAN, activo BOOLEAN, "
                + "create_by INT NULL, create_date DATETIME NULL, update_by INT NULL, update_date DATETIME NULL)");
        jdbc.execute("CREATE TABLE rol (id_rol INT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(50) NOT NULL UNIQUE)");
        jdbc.execute("CREATE TABLE rol_permiso (id_rol INT NOT NULL, id_permiso INT NOT NULL, PRIMARY KEY (id_rol, id_permiso))");
        jdbc.update("INSERT INTO rol (codigo) VALUES ('ADMIN'), ('GERENTE')");

        jdbc.update("INSERT INTO cuentas_contables (id, codigo, nombre) VALUES (5, '5.1.01', 'Compras'), (60, '5.1.03.001', 'Servicios')");
        jdbc.update("INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id) VALUES "
                + "('CPP_CUENTA_COMPRAS', 'Débito compras a proveedores', 5)");
        // 1: con cuenta de gasto y dos filas de desglose; 2: sin cuenta de gasto (CxP de OC) y una fila sin tarifa;
        // 3: descripción de 300 caracteres
        jdbc.update("INSERT INTO cuentas_por_pagar (id, numero, descripcion, monto, cuenta_gasto_id) VALUES "
                + "(1, 'CPP-2026-00001', 'Servicios varios', 123000.00, 60), "
                + "(2, 'CPP-2026-00002', 'Compra según OC OC-1', 5000.00, NULL), "
                + "(3, 'CPP-2026-00003', '" + "d".repeat(300) + "', 1130.00, 60)");
        jdbc.update("INSERT INTO cuenta_pagar_iva (cuenta_por_pagar_id, codigo_tarifa, base, iva) VALUES "
                + "(1, '08', 100000.00, 13000.00), (1, '04', 10000.00, 100.00), (2, NULL, 5000.00, 0.00), (3, '08', 1000.00, 130.00)");

        aplicar();
    }

    static void aplicar() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(ARCHIVO));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(ds);
    }

    private static List<Map<String, Object>> lineas(long cxp) {
        return jdbc.queryForList("SELECT * FROM cuenta_pagar_linea WHERE cuenta_por_pagar_id = ? ORDER BY numero_linea", cxp);
    }

    @Test
    @DisplayName("una línea por fila del desglose, numeradas, con la cuenta de la CxP, tratamiento PLENO, acreditable = IVA y no acreditable 0")
    void lineasMigradas() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuenta_pagar_linea", Integer.class)).isEqualTo(4);
        List<Map<String, Object>> c1 = lineas(1);
        assertThat(c1).hasSize(2);
        assertThat(c1.get(0).get("numero_linea")).isEqualTo(1);
        assertThat(c1.get(1).get("numero_linea")).isEqualTo(2);
        assertThat(((Number) c1.get(0).get("cuenta_contable_id")).longValue()).isEqualTo(60L);
        assertThat(c1.get(0).get("codigo_tarifa")).isEqualTo("08");
        assertThat((BigDecimal) c1.get(0).get("base")).isEqualByComparingTo("100000.00");
        assertThat((BigDecimal) c1.get(0).get("iva")).isEqualByComparingTo("13000.00");
        assertThat(c1.get(1).get("codigo_tarifa")).isEqualTo("04");
        for (Map<String, Object> l : c1) {
            assertThat(l.get("tratamiento")).isEqualTo("PLENO");
            assertThat(l.get("tarifa_limite")).isNull();
            assertThat((BigDecimal) l.get("iva_acreditable")).isEqualByComparingTo((BigDecimal) l.get("iva"));
            assertThat((BigDecimal) l.get("iva_no_acreditable")).isEqualByComparingTo("0.00");
            assertThat(l.get("descripcion")).isEqualTo("Servicios varios");
        }
    }

    @Test
    @DisplayName("sin cuenta de gasto la línea usa la cuenta de CPP_CUENTA_COMPRAS (la que debitó el asiento de la OC); sin tarifa se conserva NULL")
    void cuentaPorDefectoDeCompras() {
        Map<String, Object> l = lineas(2).get(0);
        assertThat(((Number) l.get("cuenta_contable_id")).longValue()).isEqualTo(5L);
        assertThat(l.get("codigo_tarifa")).isNull();
        assertThat((BigDecimal) l.get("base")).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("la descripción de 300 caracteres cabe entera")
    void descripcionLarga() {
        assertThat((String) lineas(3).get(0).get("descripcion")).hasSize(300);
    }

    @Test
    @DisplayName("cuenta_pagar_iva desaparece; cuenta_gasto_id sigue existiendo y es nullable")
    void tablaVieja() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                + "AND table_name = 'cuenta_pagar_iva'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'cuentas_por_pagar' AND column_name = 'cuenta_gasto_id'", String.class)).isEqualTo("YES");
    }

    @Test
    @DisplayName("la línea exige tratamiento válido y cuenta existente")
    void restriccionesDeLinea() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO cuenta_pagar_linea (cuenta_por_pagar_id, numero_linea, descripcion, "
                + "cuenta_contable_id, base, iva, tratamiento, iva_acreditable, iva_no_acreditable) "
                + "VALUES (1, 9, 'x', 60, 1, 0, 'OTRO', 0, 0)")).isInstanceOf(DataAccessException.class).hasMessageContaining("ck_cpl_tratamiento");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO cuenta_pagar_linea (cuenta_por_pagar_id, numero_linea, descripcion, "
                + "cuenta_contable_id, base, iva, tratamiento, iva_acreditable, iva_no_acreditable) "
                + "VALUES (1, 9, 'x', 999, 1, 0, 'PLENO', 0, 0)")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO cuenta_pagar_linea (cuenta_por_pagar_id, numero_linea, descripcion, "
                + "cuenta_contable_id, base, iva, tratamiento, iva_acreditable, iva_no_acreditable) "
                + "VALUES (1, 1, 'repetida', 60, 1, 0, 'PLENO', 0, 0)")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("aceptación: las CxP existentes quedan PENDIENTE, sin fecha ni número")
    void aceptacionPorDefecto() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE estado_aceptacion = 'PENDIENTE' "
                + "AND fecha_aceptacion IS NULL AND numero_mensaje_receptor IS NULL", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("prorrata_iva: un porcentaje por año (único) entre 0 y 100")
    void prorrata() {
        jdbc.update("INSERT INTO prorrata_iva (anio, porcentaje, nota, usuario_id, fecha_registro, fecha_actualizacion) "
                + "VALUES (2026, 85.50, 'provisional', 1, NOW(), NOW())");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO prorrata_iva (anio, porcentaje, fecha_registro, fecha_actualizacion) "
                + "VALUES (2026, 10, NOW(), NOW())")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO prorrata_iva (anio, porcentaje, fecha_registro, fecha_actualizacion) "
                + "VALUES (2027, 100.01, NOW(), NOW())")).isInstanceOf(DataAccessException.class).hasMessageContaining("ck_prorrata_iva_porcentaje");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO prorrata_iva (anio, porcentaje, fecha_registro, fecha_actualizacion) "
                + "VALUES (2028, -0.01, NOW(), NOW())")).isInstanceOf(DataAccessException.class).hasMessageContaining("ck_prorrata_iva_porcentaje");
        jdbc.update("INSERT INTO prorrata_iva (anio, porcentaje, fecha_registro, fecha_actualizacion) VALUES (2029, 0, NOW(), NOW())");
        jdbc.update("INSERT INTO prorrata_iva (anio, porcentaje, fecha_registro, fecha_actualizacion) VALUES (2030, 100, NOW(), NOW())");
    }

    @Test
    @DisplayName("PRORRATA_IVA_GESTIONAR existe como crítico, asignado a ADMIN y no a GERENTE; reejecutar el INSERT es idempotente")
    void permisoIdempotente() throws Exception {
        String asignados = "SELECT COUNT(*) FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol "
                + "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE p.codigo = 'PRORRATA_IVA_GESTIONAR' AND r.codigo = ";
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'PRORRATA_IVA_GESTIONAR' AND es_critico = 1 "
                + "AND categoria IS NOT NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(asignados + "'ADMIN'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(asignados + "'GERENTE'", Integer.class)).isZero();

        String sql;
        try (var in = new ClassPathResource(ARCHIVO).getInputStream()) {
            sql = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        int desde = sql.indexOf("INSERT IGNORE INTO permiso");
        int hasta = sql.indexOf("-- fin-permiso");
        assertThat(desde).isPositive();
        assertThat(hasta).isGreaterThan(desde);
        for (String stmt : sql.substring(desde, hasta).split(";")) {
            if (!stmt.isBlank()) {
                jdbc.execute(stmt);
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'PRORRATA_IVA_GESTIONAR'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(asignados + "'ADMIN'", Integer.class)).isEqualTo(1);
    }
}
