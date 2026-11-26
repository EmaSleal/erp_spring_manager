package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import db.migration.V60__factura_regimen_emisor;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-057 (criterios 2, 19 y 20; revisión 1): V60 sobre MySQL 8.0.32, en una tabla factura mínima con la tabla
 * regimen_tributario sembrada como en V58. Agrega factura.regimen_emisor NOT NULL y rellena cada factura con el régimen del
 * tramo de mayor vigente_desde menor o igual a DATE(create_date), sin ids, fechas ni regímenes literales en la migración.
 * Las fechas de las facturas de estas pruebas se toman de las filas de regimen_tributario, no están fijas aquí.
 */
@Testcontainers
@DisplayName("T-057 — migración V60: factura.regimen_emisor según el tramo vigente")
class MigracionFacturaRegimenEmisorIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t057", "t057-it-mysql-migracion");

    static JdbcTemplate jdbc;
    static DriverManagerDataSource ds;

    @BeforeEach
    void esquemaYSemilla() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS factura");
        jdbc.execute("DROP TABLE IF EXISTS regimen_tributario");
        jdbc.execute("CREATE TABLE factura (id_factura INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "numero_factura VARCHAR(50), id_cliente INT NOT NULL, create_date DATETIME(6) NULL)");
        jdbc.execute("CREATE TABLE regimen_tributario (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "regimen VARCHAR(12) NOT NULL, vigente_desde DATE NOT NULL, UNIQUE KEY uk_desde (vigente_desde))");
        // semilla de V58: SIMPLIFICADO desde antes de todo documento, GENERAL desde el 01/09/2026
        jdbc.update("INSERT INTO regimen_tributario (regimen, vigente_desde) VALUES ('SIMPLIFICADO', '2000-01-01'), ('GENERAL', '2026-09-01')");
    }

    private LocalDate inicio(String regimen) {
        return jdbc.queryForObject("SELECT MAX(vigente_desde) FROM regimen_tributario WHERE regimen = ?", LocalDate.class, regimen);
    }

    private void factura(String numero, LocalDate dia, String hora) {
        jdbc.update("INSERT INTO factura (numero_factura, id_cliente, create_date) VALUES (?, 1, ?)", numero, dia + " " + hora);
    }

    private String regimen(String numero) {
        return jdbc.queryForObject("SELECT regimen_emisor FROM factura WHERE numero_factura = ?", String.class, numero);
    }

    private void migrar() {
        try (Connection con = ds.getConnection()) {
            V60__factura_regimen_emisor.aplicar(con);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("una factura dentro del tramo SIMPLIFICADO sembrado queda SIMPLIFICADO")
    void enTramoSimplificado() {
        factura("FA-JUL", inicio("GENERAL").minusMonths(2), "10:00:00");
        migrar();
        assertThat(regimen("FA-JUL")).isEqualTo("SIMPLIFICADO");
    }

    @Test
    @DisplayName("una factura en o después del inicio del tramo GENERAL queda GENERAL")
    void enTramoGeneral() {
        factura("FA-NOV", inicio("GENERAL").plusDays(70), "10:00:00");
        migrar();
        assertThat(regimen("FA-NOV")).isEqualTo("GENERAL");
    }

    @Test
    @DisplayName("frontera exacta: el día anterior al inicio GENERAL es SIMPLIFICADO y el día de inicio es GENERAL, a cualquier hora")
    void frontera() {
        LocalDate inicioGeneral = inicio("GENERAL");
        factura("FA-ANTES-INI", inicioGeneral.minusDays(1), "00:00:00");
        factura("FA-ANTES-FIN", inicioGeneral.minusDays(1), "23:59:59");
        factura("FA-INICIO", inicioGeneral, "00:00:00");
        factura("FA-INICIO-FIN", inicioGeneral, "23:59:59");
        migrar();
        assertThat(regimen("FA-ANTES-INI")).isEqualTo("SIMPLIFICADO");
        assertThat(regimen("FA-ANTES-FIN")).isEqualTo("SIMPLIFICADO");
        assertThat(regimen("FA-INICIO")).isEqualTo("GENERAL");
        assertThat(regimen("FA-INICIO-FIN")).isEqualTo("GENERAL");
    }

    @Test
    @DisplayName("sigue el último tramo con vigente_desde <= la fecha, incluso con un tramo futuro SIMPLIFICADO")
    void variosTramos() {
        LocalDate futuro = inicio("GENERAL").plusYears(1);
        jdbc.update("INSERT INTO regimen_tributario (regimen, vigente_desde) VALUES ('SIMPLIFICADO', ?)", futuro);
        factura("FA-GEN", futuro.minusDays(1), "12:00:00");
        factura("FA-FUT", futuro, "00:00:00");
        migrar();
        assertThat(regimen("FA-GEN")).isEqualTo("GENERAL");
        assertThat(regimen("FA-FUT")).isEqualTo("SIMPLIFICADO");
    }

    @Test
    @DisplayName("una factura sin tramo para su fecha hace fallar la migración y no deja la columna a medias")
    void sinTramoFalla() {
        factura("FA-OK", inicio("GENERAL"), "10:00:00");
        factura("FA-SIN-TRAMO", jdbc.queryForObject("SELECT MIN(vigente_desde) FROM regimen_tributario", LocalDate.class).minusDays(1), "10:00:00");
        assertThatThrownBy(this::migrar).hasMessageContaining("V60").hasMessageContaining("sin tramo");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'factura' AND column_name = 'regimen_emisor'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("es idempotente: aplicarla otra vez no cambia nada")
    void idempotente() {
        factura("FA-1", inicio("GENERAL").minusDays(1), "10:00:00");
        factura("FA-2", inicio("GENERAL"), "10:00:00");
        migrar();
        migrar();
        assertThat(regimen("FA-1")).isEqualTo("SIMPLIFICADO");
        assertThat(regimen("FA-2")).isEqualTo("GENERAL");
    }

    @Test
    @DisplayName("la columna es NOT NULL, sin valor por defecto, y solo admite GENERAL o SIMPLIFICADO")
    void restricciones() {
        factura("FA-1", inicio("GENERAL"), "10:00:00");
        migrar();
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'factura' AND column_name = 'regimen_emisor'", String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'factura' AND column_name = 'regimen_emisor'", String.class)).isNull();
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO factura (numero_factura, id_cliente, create_date) VALUES ('FA-SIN', 1, NOW())"));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO factura (numero_factura, id_cliente, create_date, regimen_emisor) VALUES ('FA-X', 1, NOW(), 'OTRO')"));
    }
}
