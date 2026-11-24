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
 * T-055: el SQL real de V57 sobre MySQL 8.0.32, sobre tablas mínimas: la tabla de entregas, el índice único que impide
 * más de una entrega AUTOMATICO por comprobante, las claves foráneas y el permiso nuevo asignado a ADMIN.
 */
@Testcontainers
@DisplayName("T-055 — migración V57: entrega_comprobante y permiso COMPROBANTE_REENVIAR_CORREO")
class MigracionEntregaComprobanteIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t055", "t055-it-mysql-migracion");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE comprobante_electronico (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE usuario (id_usuario INT NOT NULL AUTO_INCREMENT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE rol (id_rol INT NOT NULL AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE permiso (id_permiso INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "codigo VARCHAR(100) NOT NULL UNIQUE, nombre VARCHAR(100), descripcion VARCHAR(500), "
                + "categoria VARCHAR(50), es_critico BIT, activo BIT, create_by INT, create_date DATETIME, "
                + "update_by INT, update_date DATETIME)");
        jdbc.execute("CREATE TABLE rol_permiso (id_rol INT NOT NULL, id_permiso INT NOT NULL, "
                + "PRIMARY KEY (id_rol, id_permiso))");
        jdbc.update("INSERT INTO rol (codigo) VALUES ('ADMIN'), ('USER')");
        jdbc.update("INSERT INTO comprobante_electronico VALUES (1), (2)");
        jdbc.update("INSERT INTO usuario VALUES (1)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V57__entrega_comprobante_correo.sql")).execute(ds);
    }

    private static void insertar(long comprobante, String origen, Long clave, String estado, Integer usuario) {
        jdbc.update("INSERT INTO entrega_comprobante (comprobante_id, tipo_documento, origen, usuario_id, estado, intentos, "
                + "fecha_creacion, clave_automatica) VALUES (?, 'FACTURA', ?, ?, ?, 0, '2026-11-01 10:00:00', ?)",
                comprobante, origen, usuario, estado, clave);
    }

    @Test
    @DisplayName("el índice único impide una segunda entrega AUTOMATICO del mismo comprobante, pero admite varias MANUAL")
    void unicaAutomaticaPorComprobante() {
        insertar(1, "AUTOMATICO", 1L, "PENDIENTE", null);

        assertThrows(DataAccessException.class, () -> insertar(1, "AUTOMATICO", 1L, "PENDIENTE", null));

        insertar(1, "MANUAL", null, "PENDIENTE", 1);
        insertar(1, "MANUAL", null, "PENDIENTE", 1);
        insertar(2, "AUTOMATICO", 2L, "OMITIDO_SIN_CORREO", null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrega_comprobante WHERE comprobante_id = 1", Integer.class))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("claves foráneas al comprobante y al usuario; estado y origen restringidos")
    void restricciones() {
        assertThrows(DataAccessException.class, () -> insertar(999, "MANUAL", null, "PENDIENTE", null));
        assertThrows(DataAccessException.class, () -> insertar(1, "MANUAL", null, "PENDIENTE", 999));
        assertThrows(DataAccessException.class, () -> insertar(1, "MANUAL", null, "INVENTADO", null));
        assertThrows(DataAccessException.class, () -> insertar(1, "OTRO", null, "PENDIENTE", null));
    }

    @Test
    @DisplayName("el permiso se crea una vez y se asigna solo a ADMIN")
    void permiso() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'COMPROBANTE_REENVIAR_CORREO'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT r.codigo FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol "
                + "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE p.codigo = 'COMPROBANTE_REENVIAR_CORREO'",
                String.class)).containsExactly("ADMIN");
    }
}
