package db.migration;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-050 (criterio 8, ADR-060): V51 sobre MySQL 8.0.32 con el servidor en America/Costa_Rica y datos sembrados como si la
 * base siguiera en UTC (sesión +00:00): corrige solo las columnas TIMESTAMP escritas por la aplicación, deja las de la
 * base (DEFAULT/ON UPDATE), las MIXTA y las DATETIME, aborta si el servidor sigue en UTC y no falla sobre una base vacía.
 *
 * Bases: MySQL en Testcontainers (t050-it-mysql-v51, zona nueva; t050-it-mysql-v51-utc, zona UTC); tmpfs.
 */
@Testcontainers
@DisplayName("T-050 — migración V51: corrección única de los TIMESTAMP de la aplicación")
class V51ZonaTimestampsIT {

    @Container
    static final MySQLContainer<?> CR = MysqlZonaCostaRica.contenedor("t050v51", "t050-it-mysql-v51");

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> UTC = new MySQLContainer<>(MysqlZonaCostaRica.IMAGEN)
            .withDatabaseName("t050v51")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t050-it-mysql-v51-utc"));

    static Connection abrir(MySQLContainer<?> c, boolean root, String base) throws SQLException {
        String url = c.getJdbcUrl().replace("/" + c.getDatabaseName(), "/" + base);
        return DriverManager.getConnection(url, root ? "root" : c.getUsername(), c.getPassword());
    }

    /** Esquema mínimo y datos como los dejó la base en UTC: la app guardó 06:12:40 (hora local) como si fuera UTC. */
    static void sembrarComoEnUtc(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("DROP TABLE IF EXISTS persistent_logins");
            st.execute("DROP TABLE IF EXISTS flyway_schema_history_t050");
            st.execute("DROP TABLE IF EXISTS saldo_vacaciones");
            st.execute("DROP TABLE IF EXISTS comprobante_electronico");
            st.execute("CREATE TABLE persistent_logins (username VARCHAR(64) NOT NULL, series VARCHAR(64) PRIMARY KEY, "
                    + "token VARCHAR(64) NOT NULL, last_used TIMESTAMP NOT NULL)");
            st.execute("CREATE TABLE flyway_schema_history_t050 (id INT PRIMARY KEY, "
                    + "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE saldo_vacaciones (id INT PRIMARY KEY, created_at TIMESTAMP NOT NULL "
                    + "DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
                    + "ON UPDATE CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE comprobante_electronico (id INT PRIMARY KEY, fecha_emision DATETIME(6) NOT NULL)");
            st.execute("SET SESSION time_zone = '+00:00'");
            st.execute("INSERT INTO persistent_logins VALUES ('u1','s1','t1','2026-08-10 06:12:40'), "
                    + "('u2','s2','t2','2026-08-27 03:23:57')");
            st.execute("INSERT INTO flyway_schema_history_t050 (id) VALUES (1)");
            st.execute("INSERT INTO saldo_vacaciones (id) VALUES (1)");
            st.execute("INSERT INTO comprobante_electronico VALUES (10, '2026-11-18 13:49:26.231523')");
            st.execute("SET SESSION time_zone = DEFAULT");
        }
    }

    private static String texto(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    @BeforeEach
    void sembrar() throws SQLException {
        try (Connection con = abrir(CR, false, "t050v51")) {
            sembrarComoEnUtc(con);
        }
    }

    @Test
    @DisplayName("con el servidor en America/Costa_Rica corrige las APP y deja BASE, MIXTA y DATETIME")
    void corrigeSoloLasApp() throws Exception {
        try (Connection con = abrir(CR, false, "t050v51")) {
            assertThat(texto(con, "SELECT @@global.time_zone")).isEqualTo("America/Costa_Rica");
            String baseAntes = texto(con, "SELECT UNIX_TIMESTAMP(installed_on) FROM flyway_schema_history_t050");
            String mixtaAntes = texto(con, "SELECT UNIX_TIMESTAMP(created_at) FROM saldo_vacaciones");
            // al cambiar la zona, lo sembrado en UTC se lee 6 h antes
            assertThat(texto(con, "SELECT CAST(MIN(last_used) AS CHAR) FROM persistent_logins"))
                    .isEqualTo("2026-08-10 00:12:40");

            Map<String, Integer> filas = V51__zona_costa_rica_corregir_timestamps.aplicar(con);

            assertThat(filas).containsEntry("persistent_logins.last_used", 2);
            assertThat(filas.keySet()).containsOnly("persistent_logins.last_used");
            assertThat(texto(con, "SELECT CAST(last_used AS CHAR) FROM persistent_logins WHERE series = 's1'"))
                    .isEqualTo("2026-08-10 06:12:40");
            assertThat(texto(con, "SELECT CAST(last_used AS CHAR) FROM persistent_logins WHERE series = 's2'"))
                    .isEqualTo("2026-08-27 03:23:57");
            assertThat(texto(con, "SELECT UNIX_TIMESTAMP(installed_on) FROM flyway_schema_history_t050"))
                    .as("BASE intacta").isEqualTo(baseAntes);
            assertThat(texto(con, "SELECT UNIX_TIMESTAMP(created_at) FROM saldo_vacaciones"))
                    .as("MIXTA intacta").isEqualTo(mixtaAntes);
            assertThat(texto(con, "SELECT CAST(fecha_emision AS CHAR) FROM comprobante_electronico"))
                    .as("DATETIME intacta").isEqualTo("2026-11-18 13:49:26.231523");
        }
    }

    @Test
    @DisplayName("con la zona -06:00 también corre (equivalente: Costa Rica no tiene horario de verano)")
    void aceptaOffsetFijo() throws Exception {
        assertThat(V51__zona_costa_rica_corregir_timestamps.ZONAS_ACEPTADAS).contains("America/Costa_Rica", "-06:00");
    }

    @Test
    @DisplayName("con el servidor aún en UTC aborta con un mensaje claro y no cambia nada")
    void conZonaUtc_abortaSinCambiar() throws Exception {
        try (Connection con = abrir(UTC, false, "t050v51")) {
            sembrarComoEnUtc(con);
            assertThat(texto(con, "SELECT @@global.time_zone")).isIn("SYSTEM", "UTC", "+00:00");
            String antes = texto(con, "SELECT GROUP_CONCAT(CAST(last_used AS CHAR) ORDER BY series) FROM persistent_logins");

            assertThatThrownBy(() -> V51__zona_costa_rica_corregir_timestamps.aplicar(con))
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("America/Costa_Rica")
                    .hasMessageContaining("--default-time-zone");

            assertThat(texto(con, "SELECT GROUP_CONCAT(CAST(last_used AS CHAR) ORDER BY series) FROM persistent_logins"))
                    .isEqualTo(antes);
        }
    }

    @Test
    @DisplayName("una columna APP que no es TIMESTAMP (DATETIME) se omite y sus valores no cambian")
    void columnaDatetime_seOmite() throws Exception {
        try (Connection con = abrir(CR, false, "t050v51");
             Statement st = con.createStatement()) {
            st.execute("DROP TABLE persistent_logins");
            st.execute("CREATE TABLE persistent_logins (username VARCHAR(64) NOT NULL, series VARCHAR(64) PRIMARY KEY, "
                    + "token VARCHAR(64) NOT NULL, last_used DATETIME NOT NULL)");
            st.execute("INSERT INTO persistent_logins VALUES ('u1','s1','t1','2026-08-10 06:12:40')");

            Map<String, Integer> filas = V51__zona_costa_rica_corregir_timestamps.aplicar(con);

            assertThat(filas).doesNotContainKey("persistent_logins.last_used").isEmpty();
            assertThat(texto(con, "SELECT CAST(last_used AS CHAR) FROM persistent_logins")).isEqualTo("2026-08-10 06:12:40");
        }
    }

    @Test
    @DisplayName("sobre una base vacía (sin las tablas) no modifica nada y no falla")
    void baseVacia() throws Exception {
        try (Connection root = abrir(CR, true, "t050v51");
             Statement st = root.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS t050vacia");
        }
        try (Connection con = abrir(CR, true, "t050vacia")) {
            Map<String, Integer> filas = V51__zona_costa_rica_corregir_timestamps.aplicar(con);
            assertThat(filas).isEmpty();
        }
    }
}
