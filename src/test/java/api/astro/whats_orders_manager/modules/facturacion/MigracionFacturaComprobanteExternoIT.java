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
 * T-057 (criterio 15): el SQL real de V61 sobre MySQL 8.0.32, en una tabla factura mínima. Agrega la clave del comprobante
 * emitido fuera del ERP (VARCHAR(50), única si no es nula) y su fecha (DATE); las facturas existentes quedan sin clave.
 */
@Testcontainers
@DisplayName("T-057 — migración V61: factura.comprobante_externo_clave y _fecha")
class MigracionFacturaComprobanteExternoIT {

    static final String CLAVE = "50628122600310123456700100001010000000011100000001";

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t057ce", "t057-it-mysql-ce-migracion");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE factura (id_factura INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "numero_factura VARCHAR(50), id_cliente INT NOT NULL)");
        jdbc.update("INSERT INTO factura (numero_factura, id_cliente) VALUES ('FA-1', 1), ('FA-2', 1)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V61__factura_comprobante_externo.sql")).execute(ds);
    }

    @Test
    @DisplayName("las facturas existentes quedan sin clave ni fecha; las columnas admiten NULL")
    void existentesSinClave() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE comprobante_externo_clave IS NULL "
                + "AND comprobante_externo_fecha IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT column_type FROM information_schema.columns WHERE table_schema = 't057ce' "
                + "AND table_name = 'factura' AND column_name = 'comprobante_externo_clave'", String.class))
                .isEqualTo("varchar(50)");
        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_schema = 't057ce' "
                + "AND table_name = 'factura' AND column_name = 'comprobante_externo_fecha'", String.class)).isEqualTo("date");
    }

    @Test
    @DisplayName("la clave es única entre facturas, pero varias facturas pueden no tenerla")
    void claveUnica() {
        jdbc.update("UPDATE factura SET comprobante_externo_clave = ?, comprobante_externo_fecha = '2026-12-28' "
                + "WHERE numero_factura = 'FA-1'", CLAVE);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE factura SET comprobante_externo_clave = ? WHERE numero_factura = 'FA-2'", CLAVE));
        jdbc.update("INSERT INTO factura (numero_factura, id_cliente) VALUES ('FA-3', 1), ('FA-4', 1)");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE comprobante_externo_clave IS NULL", Integer.class))
                .isGreaterThanOrEqualTo(3);
    }
}
