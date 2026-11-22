package api.astro.whats_orders_manager.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-050 (criterio 5, ADR-060): con el servidor MySQL en America/Costa_Rica, la JVM en la misma zona y la URL que usa
 * producción tras el cambio, una hora local escrita por la aplicación se lee igual tanto en TIMESTAMP como en DATETIME,
 * y NOW() de la base coincide con la hora de la aplicación.
 *
 * Base: MySQL 8 en Testcontainers (t050-it-mysql-zona, tmpfs); esquema mínimo propio.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@Testcontainers
@DisplayName("T-050 — ida y vuelta de fechas con MySQL en America/Costa_Rica")
class ZonaMysqlIdaYVueltaIT {

    private static TimeZone zonaOriginal;

    @BeforeAll
    static void zonaCostaRica() {
        zonaOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(MysqlZonaCostaRica.ZONA));
    }

    @AfterAll
    static void restaurarZona() {
        TimeZone.setDefault(zonaOriginal);
    }

    @Container
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t050", "t050-it-mysql-zona");

    /** La URL de producción tras T-050 (ver docs/despliegue/ZONA_HORARIA_MYSQL.md, «Parámetros de la DB_URL»). */
    static final String PARAMETROS_URL = "serverTimezone=" + MysqlZonaCostaRica.ZONA;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
                + (MYSQL.getJdbcUrl().contains("?") ? "&" : "?") + PARAMETROS_URL);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    @Test
    @DisplayName("el servidor corre en America/Costa_Rica (o -06:00)")
    void zonaDelServidor() {
        String zona = jdbc.queryForObject("SELECT @@global.time_zone", String.class);
        assertThat(zona).isIn(MysqlZonaCostaRica.ZONA, "-06:00");
    }

    @Test
    @DisplayName("un LocalDateTime de las 20:00 se lee 20:00 en TIMESTAMP y en DATETIME (LocalDateTime y Timestamp)")
    void idaYVuelta2000() {
        jdbc.execute("DROP TABLE IF EXISTS t050_ida_vuelta");
        jdbc.execute("CREATE TABLE t050_ida_vuelta (id INT PRIMARY KEY, ts TIMESTAMP NULL, dt DATETIME NULL)");
        LocalDateTime veinte = LocalDateTime.of(2026, 11, 21, 20, 0, 0);

        jdbc.update("INSERT INTO t050_ida_vuelta (id, ts, dt) VALUES (1, ?, ?)", veinte, veinte);
        jdbc.update("INSERT INTO t050_ida_vuelta (id, ts, dt) VALUES (2, ?, ?)",
                Timestamp.valueOf(veinte), Timestamp.valueOf(veinte));

        for (int id : new int[] {1, 2}) {
            assertThat(jdbc.queryForObject("SELECT CAST(ts AS CHAR) FROM t050_ida_vuelta WHERE id = ?", String.class, id))
                    .as("TIMESTAMP guardado (texto de la base), fila %d", id).isEqualTo("2026-11-21 20:00:00");
            assertThat(jdbc.queryForObject("SELECT CAST(dt AS CHAR) FROM t050_ida_vuelta WHERE id = ?", String.class, id))
                    .as("DATETIME guardado (texto de la base), fila %d", id).isEqualTo("2026-11-21 20:00:00");
            assertThat(jdbc.queryForObject("SELECT ts FROM t050_ida_vuelta WHERE id = ?", Timestamp.class, id)
                    .toLocalDateTime()).as("TIMESTAMP leído, fila %d", id).isEqualTo(veinte);
            assertThat(jdbc.queryForObject("SELECT dt FROM t050_ida_vuelta WHERE id = ?", Timestamp.class, id)
                    .toLocalDateTime()).as("DATETIME leído, fila %d", id).isEqualTo(veinte);
            assertThat(jdbc.queryForObject("SELECT ts FROM t050_ida_vuelta WHERE id = ?", LocalDateTime.class, id))
                    .as("TIMESTAMP como LocalDateTime, fila %d", id).isEqualTo(veinte);
        }
        // internamente el TIMESTAMP guarda el instante real: las 20:00 de Costa Rica son las 02:00 UTC del día siguiente
        assertThat(jdbc.queryForObject("SELECT CAST(CONVERT_TZ(ts, @@global.time_zone, '+00:00') AS CHAR) "
                + "FROM t050_ida_vuelta WHERE id = 1", String.class)).isEqualTo("2026-11-22 02:00:00");
        jdbc.execute("DROP TABLE t050_ida_vuelta");
    }

    @Test
    @DisplayName("NOW() de la base coincide con LocalDateTime.now(clock) de la aplicación (tolerancia de 1 minuto)")
    void nowDeLaBaseCoincideConElClock() {
        LocalDateTime base = jdbc.queryForObject("SELECT NOW()", LocalDateTime.class);
        LocalDateTime app = LocalDateTime.now(clock);
        assertThat(Duration.between(base, app).abs()).isLessThan(Duration.ofMinutes(1));
    }
}
