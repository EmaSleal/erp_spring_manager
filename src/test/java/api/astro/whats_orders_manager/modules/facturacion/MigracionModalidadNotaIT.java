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
 * T-051: el SQL real de V52 sobre MySQL 8.0.32, en una tabla mínima. La modalidad de las notas existentes sale de su
 * código (02 y 14 → PARCIAL, el resto → TOTAL) y la columna queda NOT NULL con los dos únicos valores.
 */
@Testcontainers
@DisplayName("T-051 — migración V52: modalidad de la nota de crédito")
class MigracionModalidadNotaIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t051", "t051-it-mysql-migraciones");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE nota_factura (id BIGINT NOT NULL PRIMARY KEY, codigo_referencia VARCHAR(2) NOT NULL)");
        for (String[] n : new String[][] {{"1", "01"}, {"2", "02"}, {"3", "13"}, {"4", "14"}, {"5", "02"}}) {
            jdbc.update("INSERT INTO nota_factura VALUES (?, ?)", Long.parseLong(n[0]), n[1]);
        }
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V52__nota_factura_modalidad.sql")).execute(ds);
    }

    private static String modalidad(long id) {
        return jdbc.queryForObject("SELECT modalidad FROM nota_factura WHERE id = ?", String.class, id);
    }

    @Test
    @DisplayName("backfill: 01 y 13 → TOTAL; 02 y 14 → PARCIAL")
    void backfill() {
        assertThat(modalidad(1)).isEqualTo("TOTAL");
        assertThat(modalidad(2)).isEqualTo("PARCIAL");
        assertThat(modalidad(3)).isEqualTo("TOTAL");
        assertThat(modalidad(4)).isEqualTo("PARCIAL");
        assertThat(modalidad(5)).isEqualTo("PARCIAL");
    }

    @Test
    @DisplayName("la columna es NOT NULL y solo admite TOTAL o PARCIAL")
    void restricciones() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO nota_factura (id, codigo_referencia, modalidad) VALUES (90, '06', NULL)"));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO nota_factura (id, codigo_referencia, modalidad) VALUES (91, '06', 'OTRA')"));
        jdbc.update("INSERT INTO nota_factura (id, codigo_referencia, modalidad) VALUES (92, '06', 'PARCIAL')");
        jdbc.update("INSERT INTO nota_factura (id, codigo_referencia, modalidad) VALUES (93, '06', 'TOTAL')");
        assertThat(modalidad(92)).isEqualTo("PARCIAL");
        assertThat(modalidad(93)).isEqualTo("TOTAL");
    }
}
