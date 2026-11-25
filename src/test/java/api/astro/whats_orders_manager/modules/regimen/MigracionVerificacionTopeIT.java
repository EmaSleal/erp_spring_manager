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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-056 parte 2 (criterios 5 y 7): el SQL real de V59 sobre MySQL 8.0.32. Crea verificacion_regimen y tope_compras_rts (con
 * el multiplicador 186 y los umbrales 80 y 100 como valores por defecto de la columna y sin sembrar ningún año), restringe
 * resultados, años y umbrales, asigna el permiso nuevo solo a ADMIN y se puede aplicar dos veces.
 */
@Testcontainers
@DisplayName("T-056 — migración V59: verificacion_regimen, tope_compras_rts y permiso REGIMEN_TOPE_COMPRAS_GESTIONAR")
class MigracionVerificacionTopeIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t056b", "t056b-it-mysql-migracion");

    static final String V59 = "db/migration/V59__verificacion_regimen_y_tope_compras_rts.sql";

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
        new ResourceDatabasePopulator(new ClassPathResource(V59)).execute(ds);
    }

    @Test
    @DisplayName("tope_compras_rts: sin años sembrados; multiplicador, umbrales y nivel salen del DEFAULT de la columna")
    void valoresPorDefectoDeLaColumna() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tope_compras_rts", Integer.class)).isZero();
        jdbc.update("INSERT INTO tope_compras_rts (anio, salario_base, fecha_registro, fecha_actualizacion) "
                + "VALUES (2026, 462200.00, '2026-11-25 10:00:00', '2026-11-25 10:00:00')");
        Map<String, Object> f = jdbc.queryForMap("SELECT multiplicador, umbral_aviso, umbral_tope, nivel_notificado "
                + "FROM tope_compras_rts WHERE anio = 2026");
        assertThat(f.get("multiplicador").toString()).isEqualTo("186.00");
        assertThat(f.get("umbral_aviso").toString()).isEqualTo("80.00");
        assertThat(f.get("umbral_tope").toString()).isEqualTo("100.00");
        assertThat(f.get("nivel_notificado")).isEqualTo("BAJO");
        jdbc.update("DELETE FROM tope_compras_rts");
    }

    @Test
    @DisplayName("tope_compras_rts: año único, salario y multiplicador positivos, umbrales 0 < aviso < tope <= 100")
    void restriccionesDelTope() {
        String base = "INSERT INTO tope_compras_rts (anio, salario_base, multiplicador, umbral_aviso, umbral_tope, fecha_registro, "
                + "fecha_actualizacion) VALUES ";
        String horas = ", '2026-11-25 10:00:00', '2026-11-25 10:00:00')";
        jdbc.update(base + "(2031, 1, 186, 80, 100" + horas);
        assertThrows(DataAccessException.class, () -> jdbc.update(base + "(2031, 1, 186, 80, 100" + horas));
        assertThrows(DataAccessException.class, () -> jdbc.update(base + "(2032, 0, 186, 80, 100" + horas));
        assertThrows(DataAccessException.class, () -> jdbc.update(base + "(2032, 1, 0, 80, 100" + horas));
        assertThrows(DataAccessException.class, () -> jdbc.update(base + "(2032, 1, 186, 100, 80" + horas));
        assertThrows(DataAccessException.class, () -> jdbc.update(base + "(2032, 1, 186, 80, 101" + horas));
        assertThrows(DataAccessException.class, () -> jdbc.update(base + "(2032, 1, 186, 0, 100" + horas));
        jdbc.update("DELETE FROM tope_compras_rts");
    }

    @Test
    @DisplayName("verificacion_regimen: el resultado solo admite los cuatro valores")
    void restriccionesDeLaVerificacion() {
        jdbc.update("INSERT INTO verificacion_regimen (fecha_hora, regimen_local, codigo_hacienda, resultado) "
                + "VALUES ('2026-11-25 06:30:00', 'GENERAL', 1, 'COINCIDE')");
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO verificacion_regimen (fecha_hora, resultado) "
                + "VALUES ('2026-11-25 06:30:00', 'OTRO')"));
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO verificacion_regimen (fecha_hora, resultado) "
                + "VALUES (NULL, 'COINCIDE')"));
        jdbc.update("DELETE FROM verificacion_regimen");
    }

    @Test
    @DisplayName("aplicarla otra vez no falla ni duplica el permiso (idempotente)")
    void idempotente() {
        aplicar();
        aplicar();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'REGIMEN_TOPE_COMPRAS_GESTIONAR'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rol_permiso", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("el permiso es crítico, de Configuración y se asigna solo a ADMIN")
    void permiso() {
        assertThat(jdbc.queryForObject("SELECT categoria FROM permiso WHERE codigo = 'REGIMEN_TOPE_COMPRAS_GESTIONAR'",
                String.class)).isEqualTo("Configuración");
        assertThat(jdbc.queryForObject("SELECT es_critico FROM permiso WHERE codigo = 'REGIMEN_TOPE_COMPRAS_GESTIONAR'",
                Boolean.class)).isTrue();
        assertThat(jdbc.queryForList("SELECT r.codigo FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol "
                + "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE p.codigo = 'REGIMEN_TOPE_COMPRAS_GESTIONAR'",
                String.class)).containsExactly("ADMIN");
    }
}
