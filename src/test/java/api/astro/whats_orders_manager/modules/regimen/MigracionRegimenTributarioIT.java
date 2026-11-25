package api.astro.whats_orders_manager.modules.regimen;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-056 (criterio 1): el SQL real de V58 sobre MySQL 8.0.32. Crea regimen_tributario, siembra los dos tramos
 * (SIMPLIFICADO desde 2000-01-01 y GENERAL desde 2026-09-01, cada uno con su nota; no el de 2027), restringe el
 * régimen y la fecha única, asigna el permiso nuevo solo a ADMIN y se puede aplicar dos veces sin duplicar nada.
 */
@Testcontainers
@DisplayName("T-056 — migración V58: regimen_tributario, semilla y permiso REGIMEN_TRIBUTARIO_GESTIONAR")
class MigracionRegimenTributarioIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t056", "t056-it-mysql-migracion");

    static final String V58 = "db/migration/V58__regimen_tributario_con_vigencia.sql";

    static DriverManagerDataSource ds;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE rol (id_rol INT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE permiso (id_permiso INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "codigo VARCHAR(100) NOT NULL UNIQUE, nombre VARCHAR(100), descripcion VARCHAR(500), "
                + "categoria VARCHAR(50), es_critico BIT, activo BIT, create_by INT, create_date DATETIME, "
                + "update_by INT, update_date DATETIME)");
        jdbc.execute("CREATE TABLE rol_permiso (id_rol INT NOT NULL, id_permiso INT NOT NULL, "
                + "PRIMARY KEY (id_rol, id_permiso))");
        jdbc.update("INSERT INTO rol (codigo) VALUES ('ADMIN'), ('GERENTE')");
        aplicar();
    }

    static void aplicar() {
        new ResourceDatabasePopulator(new ClassPathResource(V58)).execute(ds);
    }

    @Test
    @DisplayName("siembra SIMPLIFICADO desde 2000-01-01 y GENERAL desde 2026-09-01, con nota; no hay tramo de 2027")
    void semilla() {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT regimen, vigente_desde, nota, usuario_id FROM regimen_tributario ORDER BY vigente_desde");
        assertThat(filas).hasSize(2);
        assertThat(filas.get(0).get("regimen")).isEqualTo("SIMPLIFICADO");
        assertThat(filas.get(0).get("vigente_desde").toString()).isEqualTo("2000-01-01");
        assertThat(filas.get(1).get("regimen")).isEqualTo("GENERAL");
        assertThat(filas.get(1).get("vigente_desde").toString()).isEqualTo("2026-09-01");
        assertThat(filas).allSatisfy(f -> assertThat((String) f.get("nota")).isNotBlank());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM regimen_tributario WHERE vigente_desde >= '2027-01-01'",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("aplicarla otra vez no duplica tramos ni el permiso (idempotente)")
    void idempotente() {
        aplicar();
        aplicar();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM regimen_tributario", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'REGIMEN_TRIBUTARIO_GESTIONAR'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rol_permiso", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("la fecha de inicio es única, el régimen solo admite GENERAL o SIMPLIFICADO y la fecha y el régimen son obligatorios")
    void restricciones() {
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO regimen_tributario "
                + "(regimen, vigente_desde, fecha_registro) VALUES ('GENERAL', '2026-09-01', '2026-11-25 10:00:00')"));
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO regimen_tributario "
                + "(regimen, vigente_desde, fecha_registro) VALUES ('OTRO', '2030-01-01', '2026-11-25 10:00:00')"));
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO regimen_tributario "
                + "(regimen, vigente_desde, fecha_registro) VALUES ('GENERAL', NULL, '2026-11-25 10:00:00')"));
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO regimen_tributario "
                + "(regimen, vigente_desde, fecha_registro) VALUES (NULL, '2030-01-01', '2026-11-25 10:00:00')"));
    }

    @Test
    @DisplayName("el permiso es crítico, de Configuración y se asigna solo a ADMIN")
    void permiso() {
        Map<String, Object> p = jdbc.queryForMap("SELECT categoria, es_critico, activo FROM permiso "
                + "WHERE codigo = 'REGIMEN_TRIBUTARIO_GESTIONAR'");
        assertThat(p.get("categoria")).isEqualTo("Configuración");
        assertThat(jdbc.queryForObject("SELECT es_critico FROM permiso WHERE codigo = 'REGIMEN_TRIBUTARIO_GESTIONAR'",
                Boolean.class)).isTrue();
        assertThat(jdbc.queryForList("SELECT r.codigo FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol "
                + "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE p.codigo = 'REGIMEN_TRIBUTARIO_GESTIONAR'",
                String.class)).containsExactly("ADMIN");
    }
}
