package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-057 (criterio 2): el SQL real de V60 sobre MySQL 8.0.32, en una tabla factura mínima con facturas anteriores a la
 * migración. Agrega factura.regimen_emisor NOT NULL (GENERAL o SIMPLIFICADO), rellena las existentes con GENERAL (así se
 * calcularon) y no deja un valor por defecto: toda factura nueva debe traer su régimen sellado.
 */
@Testcontainers
@DisplayName("T-057 — migración V60: factura.regimen_emisor")
class MigracionFacturaRegimenEmisorIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t057", "t057-it-mysql-migracion");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE factura (id_factura INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "numero_factura VARCHAR(50), id_cliente INT NOT NULL, anulada TINYINT(1) NOT NULL DEFAULT 0)");
        // facturas de julio y agosto de 2026 (la semilla de V58 las ubica en el tramo SIMPLIFICADO, pero se calcularon con IVA)
        jdbc.update("INSERT INTO factura (numero_factura, id_cliente) VALUES ('FA-1', 1), ('FA-2', 1), ('FA-3', 1)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V60__factura_regimen_emisor.sql")).execute(ds);
    }

    @Test
    @DisplayName("las facturas existentes quedan GENERAL")
    void existentesGeneral() {
        assertThat(jdbc.queryForList("SELECT DISTINCT regimen_emisor FROM factura WHERE numero_factura IN ('FA-1', 'FA-2', 'FA-3')",
                String.class)).containsExactly("GENERAL");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE numero_factura IN ('FA-1', 'FA-2', 'FA-3')",
                Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("la columna es NOT NULL y sin valor por defecto: un INSERT sin régimen falla")
    void obligatoria() {
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = 't057' "
                + "AND table_name = 'factura' AND column_name = 'regimen_emisor'", String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = 't057' "
                + "AND table_name = 'factura' AND column_name = 'regimen_emisor'", String.class)).isNull();
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO factura (numero_factura, id_cliente) VALUES ('FA-SIN', 1)"));
    }

    @Test
    @DisplayName("solo admite GENERAL o SIMPLIFICADO")
    void soloDosValores() {
        jdbc.update("INSERT INTO factura (numero_factura, id_cliente, regimen_emisor) VALUES ('FA-RTS', 1, 'SIMPLIFICADO')");
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO factura (numero_factura, id_cliente, regimen_emisor) VALUES ('FA-X', 1, 'OTRO')"));
        assertThat(jdbc.queryForObject("SELECT regimen_emisor FROM factura WHERE numero_factura = 'FA-RTS'", String.class))
                .isEqualTo("SIMPLIFICADO");
    }
}
