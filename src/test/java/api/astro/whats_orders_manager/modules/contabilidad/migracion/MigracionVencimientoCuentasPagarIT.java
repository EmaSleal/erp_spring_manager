package api.astro.whats_orders_manager.modules.contabilidad.migracion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-053 revisión 1 (criterio 15): el SQL real de V56 sobre MySQL 8.0.32. Rellena fecha_vencimiento NULL de las CxP con
 * la fecha del comprobante (o, en las anteriores a V53, la de emisión) más los días de crédito del proveedor; no toca las
 * que ya la tienen; es idempotente.
 */
@Testcontainers
@DisplayName("T-053 — migración V56: fecha de vencimiento de las CxP existentes")
class MigracionVencimientoCuentasPagarIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t053v", "t053-it-mysql-venc");

    static final String ARCHIVO = "db/migration/V56__cuentas_pagar_fecha_vencimiento.sql";
    static JdbcTemplate jdbc;
    static DriverManagerDataSource ds;

    @BeforeAll
    static void migrar() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE proveedores (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, dias_credito INT NULL)");
        jdbc.execute("CREATE TABLE cuentas_por_pagar (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, proveedor_id BIGINT NOT NULL, "
                + "fecha_emision DATE NOT NULL, fecha_comprobante DATE NULL, fecha_vencimiento DATE NULL)");
        jdbc.update("INSERT INTO proveedores (id, dias_credito) VALUES (1, 30), (2, 0), (3, NULL)");
        jdbc.update("INSERT INTO cuentas_por_pagar (id, proveedor_id, fecha_emision, fecha_comprobante, fecha_vencimiento) VALUES "
                + "(1, 1, '2026-09-05', '2026-09-01', NULL), "      // 30 días sobre la fecha del comprobante
                + "(2, 2, '2026-09-05', '2026-09-01', NULL), "      // 0 días: contado
                + "(3, 3, '2026-09-05', '2026-09-01', NULL), "      // sin días: contado
                + "(4, 1, '2026-09-05', '2026-09-01', '2026-12-31'), " // explícita: no se toca
                + "(5, 1, '2026-09-05', NULL, NULL)");              // anterior a V53: usa la fecha de emisión
        aplicar();
    }

    static void aplicar() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(ARCHIVO));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(ds);
    }

    private static LocalDate vence(long id) {
        return jdbc.queryForObject("SELECT fecha_vencimiento FROM cuentas_por_pagar WHERE id = ?", LocalDate.class, id);
    }

    @Test
    @DisplayName("con días de crédito: comprobante + días; con 0 o sin días: el día del comprobante")
    void rellena() {
        assertThat(vence(1)).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(vence(2)).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(vence(3)).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("la fecha explícita no se toca y la CxP sin fecha de comprobante usa la de emisión")
    void respetaYLegado() {
        assertThat(vence(4)).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(vence(5)).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    @DisplayName("ninguna queda en NULL y repetir la migración no cambia nada")
    void idempotente() {
        aplicar();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE fecha_vencimiento IS NULL", Integer.class)).isZero();
        assertThat(vence(1)).isEqualTo(LocalDate.of(2026, 10, 1));
    }
}
