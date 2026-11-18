package api.astro.whats_orders_manager.modules.facturacion;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-039 (criterio 1): el SQL real de V44 sobre MySQL 8.0.32, en una tabla factura mínima. Ningún otro IT corre Flyway
 * (usan ddl-auto=create), así que esta prueba cubre la FK sin cascada y la restricción única de «una sola sustituta no
 * anulada por original».
 */
@Testcontainers
@DisplayName("T-039 — migración V44: vínculo de sustitución y única sustituta no anulada")
class MigracionFacturaSustitutaIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t039")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t039-it-mysql-migraciones"));

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        // Las migraciones V1..V43 parten de un esquema que creó Hibernate (baseline-on-migrate), así que no se pueden
        // correr sobre una base vacía (V1 falla: «Table usuario doesn't exist»). Se crea la tabla factura mínima, con
        // las columnas que V44 usa (id_factura INT, anulada NOT NULL), y se ejecuta el SQL real de V44.
        jdbc.execute("CREATE TABLE factura (id_factura INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "numero_factura VARCHAR(50), id_cliente INT NOT NULL, anulada TINYINT(1) NOT NULL DEFAULT 0)");
        ResourceDatabasePopulator sql = new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V44__factura_sustituta.sql"));
        sql.execute(ds);
    }

    private static void factura(int id, Integer sustituye, boolean anulada) {
        jdbc.update("INSERT INTO factura (id_factura, numero_factura, id_cliente, anulada, sustituye_factura_id) "
                + "VALUES (?, ?, 1, ?, ?)", id, "FA-" + id, anulada, sustituye);
    }

    @Test
    @DisplayName("V44 agrega las columnas, la FK sin cascada y el índice único")
    void esquema() {
        List<String> columnas = jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 't039' AND table_name = 'factura' AND column_name IN "
                + "('sustituye_factura_id','razon_sustitucion','codigo_sustitucion','sustituta_vigente_de_id')",
                String.class);
        assertThat(columnas).hasSize(4);
        Map<String, Object> fk = jdbc.queryForMap("SELECT delete_rule, update_rule FROM information_schema."
                + "referential_constraints WHERE constraint_schema = 't039' AND constraint_name = 'fk_factura_sustituye'");
        assertThat(fk.get("DELETE_RULE")).isIn("RESTRICT", "NO ACTION");
        assertThat(fk.get("UPDATE_RULE")).isIn("RESTRICT", "NO ACTION");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = 't039' "
                + "AND table_name = 'factura' AND index_name = 'uk_factura_una_sustituta_vigente' AND non_unique = 0",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("una segunda sustituta no anulada de la misma original falla con clave duplicada")
    void segundaSustituta_falla() {
        factura(100, null, true);
        factura(101, 100, false);

        assertThatThrownBy(() -> factura(102, 100, false))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_factura_una_sustituta_vigente");
    }

    @Test
    @DisplayName("al anular la primera sustituta, otra nueva es posible; las facturas normales no chocan entre sí")
    void tras_anular_la_primera_se_permite_otra() {
        factura(200, null, true);
        factura(201, 200, false);
        jdbc.update("UPDATE factura SET anulada = 1 WHERE id_factura = 201");

        factura(202, 200, false);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE sustituye_factura_id = 200", Integer.class))
                .isEqualTo(2);
        factura(300, null, false);
        factura(301, null, false);
        // la sustituta de una sustituta (criterio 8) no choca con la primera
        factura(203, 202, false);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE sustituye_factura_id = 202", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("la FK impide ligar a una original inexistente y borrar una original con sustituta")
    void fk_sinCascada() {
        assertThatThrownBy(() -> factura(400, 999999, false)).isInstanceOf(DataIntegrityViolationException.class);
        factura(410, null, true);
        factura(411, 410, false);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM factura WHERE id_factura = 410"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_factura_sustituye");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE id_factura = 411", Integer.class)).isEqualTo(1);
    }
}
