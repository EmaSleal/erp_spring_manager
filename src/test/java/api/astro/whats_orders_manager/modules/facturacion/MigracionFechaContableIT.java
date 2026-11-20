package api.astro.whats_orders_manager.modules.facturacion;

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

/**
 * T-046 (criterios 1 y 2): el SQL real de V45 sobre MySQL 8.0.32, en tablas mínimas con las columnas que usa. Ningún
 * otro IT corre Flyway (usan ddl-auto=create): esta prueba cubre el relleno de notas y sustitutas existentes sin
 * tocar asientos.
 */
@Testcontainers
@DisplayName("T-046 — migración V45: fecha contable y período fiscal de atribución")
class MigracionFechaContableIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t046")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t046-it-mysql-migraciones"));

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        // V1..V44 parten de un esquema de Hibernate (no corren sobre una base vacía): tablas mínimas con lo que V45 lee
        jdbc.execute("CREATE TABLE factura (id_factura INT NOT NULL PRIMARY KEY, create_date DATETIME(6), "
                + "sustituye_factura_id INT NULL, codigo_sustitucion VARCHAR(2) NULL)");
        jdbc.execute("CREATE TABLE comprobante_electronico (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "factura_id INT NULL, estado VARCHAR(20) NOT NULL, fecha_emision DATETIME(6) NOT NULL)");
        jdbc.execute("CREATE TABLE nota_factura (id BIGINT NOT NULL PRIMARY KEY, codigo_referencia VARCHAR(2) NOT NULL, "
                + "fecha_emision DATETIME(6) NOT NULL, fecha_emision_referencia DATETIME(6) NOT NULL)");
        jdbc.execute("CREATE TABLE asientos_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, fecha DATE NOT NULL, "
                + "tipo VARCHAR(30) NOT NULL, factura_id INT NULL, nota_factura_id BIGINT NULL, "
                + "asiento_revertido_id BIGINT NULL)");

        // Notas: 1 = código 01 con reversión; 2 = código 13 con reversión fechada en el mes de la factura (ADR-037);
        // 3 = código 13 sin reversión (en trámite); 4 = código 14 sin reversión
        jdbc.update("INSERT INTO nota_factura VALUES (1, '01', '2026-11-12 10:00:00', '2026-10-30 09:00:00')");
        jdbc.update("INSERT INTO nota_factura VALUES (2, '13', '2026-11-13 10:00:00', '2026-09-29 09:00:00')");
        jdbc.update("INSERT INTO nota_factura VALUES (3, '13', '2026-11-14 10:00:00', '2026-08-31 23:59:00')");
        jdbc.update("INSERT INTO nota_factura VALUES (4, '14', '2026-11-15 10:00:00', '2026-07-02 09:00:00')");
        jdbc.update("INSERT INTO asientos_contables (id, fecha, tipo, factura_id, nota_factura_id, asiento_revertido_id) "
                + "VALUES (101, '2026-10-30', 'AUTOMATICO_VENTA', 1, NULL, NULL)");
        jdbc.update("INSERT INTO asientos_contables (id, fecha, tipo, factura_id, nota_factura_id, asiento_revertido_id) "
                + "VALUES (102, '2026-11-12', 'AUTOMATICO_VENTA', 1, 1, 101)");
        jdbc.update("INSERT INTO asientos_contables (id, fecha, tipo, factura_id, nota_factura_id, asiento_revertido_id) "
                + "VALUES (103, '2026-09-29', 'AUTOMATICO_VENTA', 2, NULL, NULL)");
        jdbc.update("INSERT INTO asientos_contables (id, fecha, tipo, factura_id, nota_factura_id, asiento_revertido_id) "
                + "VALUES (104, '2026-09-29', 'AUTOMATICO_VENTA', 2, 2, 103)");

        // Facturas: 10 = original con comprobante ACEPTADO de septiembre; 11 = sustituta 15; 12 = sustituta 07 sin
        // asiento; 13 = factura normal (no se toca)
        jdbc.update("INSERT INTO factura VALUES (10, '2026-09-29 10:00:00', NULL, NULL)");
        jdbc.update("INSERT INTO comprobante_electronico (factura_id, estado, fecha_emision) "
                + "VALUES (10, 'ACEPTADO', '2026-09-29 10:15:30')");
        jdbc.update("INSERT INTO factura VALUES (11, '2026-11-19 08:00:00', 10, '15')");
        jdbc.update("INSERT INTO asientos_contables (id, fecha, tipo, factura_id) "
                + "VALUES (201, '2026-09-29', 'AUTOMATICO_VENTA', 11)");
        jdbc.update("INSERT INTO factura VALUES (12, '2026-11-19 09:00:00', 10, '07')");
        jdbc.update("INSERT INTO factura VALUES (13, '2026-11-01 09:00:00', NULL, NULL)");

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V45__fecha_contable_y_periodo_fiscal_atribucion.sql")).execute(ds);
    }

    private static Map<String, Object> nota(long id) {
        return jdbc.queryForMap("SELECT fecha_contable, periodo_fiscal_atribucion FROM nota_factura WHERE id = ?", id);
    }

    private static Map<String, Object> factura(int id) {
        return jdbc.queryForMap("SELECT fecha_contable, periodo_fiscal_atribucion FROM factura WHERE id_factura = ?", id);
    }

    @Test
    @DisplayName("notas: fecha del asiento de reversión (o de emisión); 13 y 14 al mes de la factura, el resto al de emisión")
    void notas() {
        assertThat(nota(1)).containsEntry("fecha_contable", java.sql.Date.valueOf("2026-11-12"))
                .containsEntry("periodo_fiscal_atribucion", "2026-11");
        // la nota 13 conserva la fecha de su reversión (mes de la factura): solo se informa, no se re-fecha
        assertThat(nota(2)).containsEntry("fecha_contable", java.sql.Date.valueOf("2026-09-29"))
                .containsEntry("periodo_fiscal_atribucion", "2026-09");
        assertThat(nota(3)).containsEntry("fecha_contable", java.sql.Date.valueOf("2026-11-14"))
                .containsEntry("periodo_fiscal_atribucion", "2026-08");
        assertThat(nota(4)).containsEntry("fecha_contable", java.sql.Date.valueOf("2026-11-15"))
                .containsEntry("periodo_fiscal_atribucion", "2026-07");
    }

    @Test
    @DisplayName("sustitutas: fecha del asiento de venta (o de emisión); 15 al mes de la original, 07 al de emisión")
    void sustitutas() {
        assertThat(factura(11)).containsEntry("fecha_contable", java.sql.Date.valueOf("2026-09-29"))
                .containsEntry("periodo_fiscal_atribucion", "2026-09");
        assertThat(factura(12)).containsEntry("fecha_contable", java.sql.Date.valueOf("2026-11-19"))
                .containsEntry("periodo_fiscal_atribucion", "2026-11");
    }

    @Test
    @DisplayName("una factura que no es sustituta queda con ambas columnas nulas y los asientos no cambian")
    void normalesYAsientosIntactos() {
        assertThat(factura(13)).containsEntry("fecha_contable", null).containsEntry("periodo_fiscal_atribucion", null);
        assertThat(jdbc.queryForObject("SELECT fecha FROM asientos_contables WHERE id = 104", java.sql.Date.class))
                .isEqualTo(java.sql.Date.valueOf("2026-09-29"));
        assertThat(jdbc.queryForObject("SELECT fecha FROM asientos_contables WHERE id = 201", java.sql.Date.class))
                .isEqualTo(java.sql.Date.valueOf("2026-09-29"));
    }

    @Test
    @DisplayName("nota_factura queda con ambas columnas NOT NULL y factura con ambas nullable")
    void nulabilidad() {
        assertThat(jdbc.queryForList("SELECT is_nullable FROM information_schema.columns WHERE table_schema = 't046' "
                + "AND table_name = 'nota_factura' AND column_name IN ('fecha_contable','periodo_fiscal_atribucion')",
                String.class)).containsOnly("NO");
        assertThat(jdbc.queryForList("SELECT is_nullable FROM information_schema.columns WHERE table_schema = 't046' "
                + "AND table_name = 'factura' AND column_name IN ('fecha_contable','periodo_fiscal_atribucion')",
                String.class)).containsOnly("YES");
    }
}
