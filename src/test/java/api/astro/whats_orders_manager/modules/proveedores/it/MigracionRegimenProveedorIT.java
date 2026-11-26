package api.astro.whats_orders_manager.modules.proveedores.it;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-058 criterio 3 (ADR-074 §2): el SQL real de V62 sobre MySQL 8.0.32 con filas previas. proveedores gana regimen_hacienda
 * (NOT NULL, SIN_VERIFICAR por defecto, cinco valores) y regimen_verificado_en (nulo); cuentas_por_pagar gana
 * regimen_proveedor (NOT NULL) y las filas existentes quedan SIN_VERIFICAR; el estado de aceptación de las existentes no cambia.
 */
@Testcontainers
@DisplayName("T-058 — migración V62: régimen del proveedor y de la CxP")
class MigracionRegimenProveedorIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t058", "t058-it-mysql-migracion");

    static final String V62 = "db/migration/V62__regimen_proveedor_y_aceptacion_no_aplica.sql";

    static DriverManagerDataSource ds;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE proveedores (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, cedula VARCHAR(20) NOT NULL)");
        jdbc.execute("CREATE TABLE cuentas_por_pagar (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "estado_aceptacion VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE')");
        jdbc.update("INSERT INTO proveedores (cedula) VALUES ('3101123456')");
        jdbc.update("INSERT INTO cuentas_por_pagar (estado_aceptacion) VALUES ('PENDIENTE'), ('ACEPTADO')");
        new ResourceDatabasePopulator(new ClassPathResource(V62)).execute(ds);
    }

    @Test
    @DisplayName("proveedores: regimen_hacienda NOT NULL con SIN_VERIFICAR por defecto y regimen_verificado_en nulo")
    void proveedores() {
        Map<String, Object> p = jdbc.queryForMap("SELECT regimen_hacienda, regimen_verificado_en FROM proveedores");
        assertThat(p.get("regimen_hacienda")).isEqualTo("SIN_VERIFICAR");
        assertThat(p.get("regimen_verificado_en")).isNull();
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'proveedores' AND column_name = 'regimen_hacienda'", String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'proveedores' AND column_name = 'regimen_verificado_en'", String.class)).isEqualTo("datetime");
        for (String valor : new String[]{"GENERAL", "SIMPLIFICADO", "NO_CONTRIBUYENTE", "OTRO", "SIN_VERIFICAR"}) {
            jdbc.update("INSERT INTO proveedores (cedula, regimen_hacienda) VALUES (?, ?)", "c-" + valor, valor);
        }
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO proveedores (cedula, regimen_hacienda) VALUES ('x', 'MIXTO')"));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO proveedores (cedula, regimen_hacienda) VALUES ('y', NULL)"));
    }

    @Test
    @DisplayName("cuentas_por_pagar: regimen_proveedor NOT NULL, las existentes quedan SIN_VERIFICAR y su aceptación no cambia")
    void cuentasPorPagar() {
        assertThat(jdbc.queryForList("SELECT regimen_proveedor FROM cuentas_por_pagar", String.class))
                .containsOnly("SIN_VERIFICAR");
        assertThat(jdbc.queryForList("SELECT estado_aceptacion FROM cuentas_por_pagar ORDER BY id", String.class))
                .containsExactly("PENDIENTE", "ACEPTADO");
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'cuentas_por_pagar' AND column_name = 'regimen_proveedor'", String.class)).isEqualTo("NO");
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO cuentas_por_pagar (regimen_proveedor) VALUES ('MIXTO')"));
        jdbc.update("INSERT INTO cuentas_por_pagar (regimen_proveedor, estado_aceptacion) VALUES ('SIMPLIFICADO', 'NO_APLICA')");
    }
}
