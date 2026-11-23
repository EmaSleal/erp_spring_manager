package api.astro.whats_orders_manager.modules.contabilidad.migracion;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-052: el SQL real de V53 sobre MySQL 8.0.32, en tablas mínimas con filas como las del dump de producción. Cubre el
 * desglose de IVA de las CxP existentes (sin inventar datos, ADR-024), las columnas nuevas, el índice único del
 * comprobante, el permiso CUENTA_PAGAR_ANULAR (idempotente), el recálculo del saldo de los proveedores en CRC y el
 * nombre de la cuenta de IVA crédito fiscal.
 */
@Testcontainers
@DisplayName("T-052 — migración V53: comprobante del proveedor, desglose de IVA, anulación, saldo en CRC")
class MigracionCuentasPagarIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t052m", "t052-it-mysql-migraciones");

    static final String ARCHIVO = "db/migration/V53__cuentas_pagar_comprobante_anulacion.sql";
    static JdbcTemplate jdbc;
    static DriverManagerDataSource ds;

    @BeforeAll
    static void migrar() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE proveedores (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, saldo_pendiente DECIMAL(19,2))");
        jdbc.execute("CREATE TABLE cuentas_por_pagar (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, numero VARCHAR(20) NOT NULL, "
                + "proveedor_id BIGINT NOT NULL, monto DECIMAL(19,2) NOT NULL, saldo_pendiente DECIMAL(19,2) NOT NULL, "
                + "subtotal DECIMAL(15,2) NULL, impuesto DECIMAL(15,2) NULL, moneda VARCHAR(10) NULL, "
                + "tipo_cambio DECIMAL(18,5) NULL, estado VARCHAR(20) NOT NULL)");
        jdbc.execute("CREATE TABLE cuentas_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30) NOT NULL, "
                + "nombre VARCHAR(150) NOT NULL)");
        jdbc.execute("CREATE TABLE permiso (id_permiso INT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100) NOT NULL UNIQUE, "
                + "nombre VARCHAR(150), descripcion VARCHAR(300), categoria VARCHAR(60), es_critico BOOLEAN, activo BOOLEAN, "
                + "create_by INT NULL, create_date DATETIME NULL, update_by INT NULL, update_date DATETIME NULL)");
        jdbc.execute("CREATE TABLE rol (id_rol INT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(50) NOT NULL UNIQUE)");
        jdbc.execute("CREATE TABLE rol_permiso (id_rol INT NOT NULL, id_permiso INT NOT NULL, PRIMARY KEY (id_rol, id_permiso))");
        jdbc.update("INSERT INTO rol (codigo) VALUES ('ADMIN'), ('GERENTE')");

        // proveedores: 1 con saldo desfasado (suma sin convertir), 2 con CxP en USD, 3 sin CxP pero con saldo, 4 con una anulada
        jdbc.update("INSERT INTO proveedores (id, saldo_pendiente) VALUES (1, 999.00), (2, 0.00), (3, 50.00), (4, 10.00)");
        // 1: 13 % exacto (113000), 2: impuesto 0, 3: tasa rara, 4: sin subtotal ni impuesto, 5: USD TC 500 con 13 %
        cxp(1, 1, "113000.00", "113000.00", "100000.00", "13000.00", "CRC", null, "PENDIENTE");
        cxp(2, 1, "5000.00", "5000.00", "5000.00", "0.00", "CRC", null, "PENDIENTE");
        cxp(3, 1, "104000.00", "104000.00", "100000.00", "4000.00", "CRC", null, "PARCIAL");
        cxp(4, 1, "20000.00", "20000.00", null, null, null, null, "PENDIENTE");
        cxp(5, 2, "113.00", "113.00", "100.00", "13.00", "USD", "500.00000", "PENDIENTE");
        cxp(6, 4, "700.00", "700.00", "700.00", "0.00", "CRC", null, "ANULADA");
        // solo subtotal sin impuesto: se conserva tal cual, sin tarifa
        cxp(7, 2, "300.00", "300.00", "300.00", null, "CRC", null, "PENDIENTE");

        // El nombre correcto se arma con bytes UTF-8 (0xC3A9 = é) para no depender de la codificación de este archivo
        jdbc.update("INSERT INTO cuentas_contables (id, codigo, nombre) VALUES (1, '1.1.03', CONCAT('IVA Cr', 'dito Fiscal')), "
                + "(2, '1.1.99', CONCAT('IVA Cr', 'dito Fiscal')), "
                + "(3, '1.1.04', CONCAT('IVA Cr', CONVERT(0xC3A9 USING utf8mb4), 'dito Fiscal')), (4, '1.1.05', 'Caja')");

        aplicar();
    }

    static void cxp(long id, long prov, String monto, String saldo, String subtotal, String impuesto, String moneda,
                    String tc, String estado) {
        jdbc.update("INSERT INTO cuentas_por_pagar (id, numero, proveedor_id, monto, saldo_pendiente, subtotal, impuesto, "
                        + "moneda, tipo_cambio, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, "CPP-2026-0000" + id, prov, monto, saldo, subtotal, impuesto, moneda, tc, estado);
    }

    static void aplicar() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(ARCHIVO));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(ds);
    }

    private static Map<String, Object> desglose(long cxp) {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT codigo_tarifa, base, iva FROM cuenta_pagar_iva WHERE cuenta_por_pagar_id = ?", cxp);
        assertThat(filas).as("CxP %s: una única fila de desglose", cxp).hasSize(1);
        return filas.get(0);
    }

    @Test
    @DisplayName("las CxP existentes conservan sus valores, con los campos nuevos nulos")
    void conservanValoresYCamposNulos() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE numero_comprobante_proveedor IS NOT NULL "
                + "OR clave_comprobante IS NOT NULL OR fecha_comprobante IS NOT NULL OR comprobante_normalizado IS NOT NULL "
                + "OR anulada_por_id IS NOT NULL OR fecha_anulacion IS NOT NULL OR motivo_anulacion IS NOT NULL",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT subtotal FROM cuentas_por_pagar WHERE id = 1", BigDecimal.class))
                .isEqualByComparingTo("100000.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE id = 4 AND subtotal IS NULL "
                + "AND impuesto IS NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("desglose de las existentes: 13 % exacto → «08»; impuesto 0, tasa rara y sin datos → sin tarifa (NULL)")
    void desgloseDeLasExistentes() {
        Map<String, Object> d1 = desglose(1);
        assertThat(d1.get("codigo_tarifa")).isEqualTo("08");
        assertThat((BigDecimal) d1.get("base")).isEqualByComparingTo("100000.00");
        assertThat((BigDecimal) d1.get("iva")).isEqualByComparingTo("13000.00");

        Map<String, Object> d2 = desglose(2);
        assertThat(d2.get("codigo_tarifa")).as("impuesto 0 es ambiguo (exento o no sujeto)").isNull();
        assertThat((BigDecimal) d2.get("base")).isEqualByComparingTo("5000.00");
        assertThat((BigDecimal) d2.get("iva")).isEqualByComparingTo("0.00");

        Map<String, Object> d3 = desglose(3);
        assertThat(d3.get("codigo_tarifa")).as("4 % sobre 100000 no es 13 %").isNull();
        assertThat((BigDecimal) d3.get("iva")).isEqualByComparingTo("4000.00");

        Map<String, Object> d4 = desglose(4);
        assertThat(d4.get("codigo_tarifa")).isNull();
        assertThat((BigDecimal) d4.get("base")).as("sin subtotal ni impuesto: base = monto").isEqualByComparingTo("20000.00");
        assertThat((BigDecimal) d4.get("iva")).isEqualByComparingTo("0.00");

        assertThat(desglose(5).get("codigo_tarifa")).isEqualTo("08");
        assertThat((BigDecimal) desglose(7).get("iva")).isEqualByComparingTo("0.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuenta_pagar_iva", Integer.class)).isEqualTo(7);
    }

    @Test
    @DisplayName("índice único (proveedor, comprobante normalizado): repite → error; NULL repetido (heredadas, anuladas) → permitido")
    void indiceUnico() {
        try {
            jdbc.update("UPDATE cuentas_por_pagar SET numero_comprobante_proveedor = 'FE-1', comprobante_normalizado = 'FE-1' WHERE id = 1");
            assertThatThrownBy(() -> jdbc.update("UPDATE cuentas_por_pagar SET comprobante_normalizado = 'FE-1' WHERE id = 3"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            // otro proveedor, mismo comprobante: permitido
            jdbc.update("UPDATE cuentas_por_pagar SET comprobante_normalizado = 'FE-1' WHERE id = 5");
            // dos filas con comprobante normalizado NULL del mismo proveedor ya conviven (id 2 y 3)
        } finally {
            jdbc.update("UPDATE cuentas_por_pagar SET numero_comprobante_proveedor = NULL, comprobante_normalizado = NULL");
        }
    }

    @Test
    @DisplayName("CUENTA_PAGAR_ANULAR existe como crítico, asignado a ADMIN y no a GERENTE; reejecutar el INSERT es idempotente")
    void permisoIdempotente() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'CUENTA_PAGAR_ANULAR' AND es_critico = 1 "
                + "AND categoria IS NOT NULL", Integer.class)).isEqualTo(1);
        String asignados = "SELECT COUNT(*) FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol "
                + "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE p.codigo = 'CUENTA_PAGAR_ANULAR' AND r.codigo = ";
        assertThat(jdbc.queryForObject(asignados + "'ADMIN'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(asignados + "'GERENTE'", Integer.class)).isZero();

        // la parte del permiso corre dos veces sin duplicar ni fallar (el resto de V53 no es reejecutable: ALTER)
        String sql = new String(readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        int desde = sql.indexOf("INSERT IGNORE INTO permiso");
        int hasta = sql.indexOf("-- fin-permiso");
        assertThat(desde).isPositive();
        assertThat(hasta).isGreaterThan(desde);
        for (String stmt : sql.substring(desde, hasta).split(";")) {
            if (!stmt.isBlank()) {
                jdbc.execute(stmt);
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'CUENTA_PAGAR_ANULAR'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(asignados + "'ADMIN'", Integer.class)).isEqualTo(1);
    }

    private static byte[] readAllBytes() {
        try (var in = new ClassPathResource(ARCHIVO).getInputStream()) {
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("saldo de los proveedores recalculado en CRC: Σ saldo × TC de las CxP no anuladas")
    void saldoRecalculado() {
        assertThat(saldo(1)).as("113000 + 5000 + 104000 + 20000").isEqualByComparingTo("242000.00");
        assertThat(saldo(2)).as("113 USD × 500 + 300 CRC").isEqualByComparingTo("56800.00");
        assertThat(saldo(3)).as("sin CxP").isEqualByComparingTo("0.00");
        assertThat(saldo(4)).as("solo una anulada").isEqualByComparingTo("0.00");
    }

    private static BigDecimal saldo(long id) {
        return jdbc.queryForObject("SELECT saldo_pendiente FROM proveedores WHERE id = ?", BigDecimal.class, id);
    }

    @Test
    @DisplayName("la cuenta 1.1.03 con el nombre dañado queda «IVA Crédito Fiscal»; otra cuenta de nombre parecido y otro código no se toca")
    void nombreDeLaCuentaDeIva() {
        String esperado = "IVA Crédito Fiscal";
        assertThat(jdbc.queryForObject("SELECT nombre FROM cuentas_contables WHERE id = 1", String.class)).isEqualTo(esperado);
        assertThat(jdbc.queryForObject("SELECT nombre FROM cuentas_contables WHERE id = 2", String.class))
                .as("otro código: intacta").isEqualTo("IVA Crdito Fiscal");
        assertThat(jdbc.queryForObject("SELECT nombre FROM cuentas_contables WHERE id = 3", String.class))
                .as("ya correcta, otro código: intacta").isEqualTo(esperado);
        assertThat(jdbc.queryForObject("SELECT nombre FROM cuentas_contables WHERE id = 4", String.class)).isEqualTo("Caja");
    }
}
