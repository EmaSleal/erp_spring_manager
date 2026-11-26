package api.astro.whats_orders_manager.modules.compras;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** T-058 criterio 11: el SQL real de V63 sobre MySQL 8.0.32 con una CxP previa; tipos y longitudes iguales a los de la entidad. */
@Testcontainers
@DisplayName("T-058 — migración V63: adjunto del comprobante de la CxP")
class MigracionAdjuntoCxpIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t058a", "t058-it-mysql-migracion-adjunto");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE cuentas_por_pagar (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, numero VARCHAR(20) NOT NULL)");
        jdbc.update("INSERT INTO cuentas_por_pagar (numero) VALUES ('CPP-2026-00001')");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V63__adjunto_comprobante_cxp.sql")).execute(ds);
    }

    @Test
    @DisplayName("las CxP existentes quedan sin adjunto y las tres columnas son VARCHAR nulos de 120, 255 y 100")
    void columnas() {
        Map<String, Object> fila = jdbc.queryForMap("SELECT adjunto_ruta, adjunto_nombre_original, adjunto_tipo FROM cuentas_por_pagar");
        assertThat(fila.values()).containsOnlyNulls();
        for (Object[] c : new Object[][]{{"adjunto_ruta", 120L}, {"adjunto_nombre_original", 255L}, {"adjunto_tipo", 100L}}) {
            Map<String, Object> m = jdbc.queryForMap("SELECT data_type, is_nullable, character_maximum_length AS largo FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = 'cuentas_por_pagar' AND column_name = ?", c[0]);
            assertThat(m.get("data_type")).isEqualTo("varchar");
            assertThat(m.get("is_nullable")).isEqualTo("YES");
            assertThat(((Number) m.get("largo")).longValue()).isEqualTo(c[1]);
        }
        jdbc.update("UPDATE cuentas_por_pagar SET adjunto_ruta = 'cxp/1/x.pdf', adjunto_nombre_original = 'x.pdf', adjunto_tipo = 'application/pdf'");
    }
}
