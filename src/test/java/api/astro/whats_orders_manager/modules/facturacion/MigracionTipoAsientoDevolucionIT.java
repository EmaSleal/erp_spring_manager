package api.astro.whats_orders_manager.modules.facturacion;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-049 (review 1, criterio 19): V50 amplía el ENUM asientos_contables.tipo con AUTOMATICO_DEVOLUCION_SALDO_FAVOR
 * conservando los once valores existentes (estado V47). La tabla se crea con la definición de producción (dump facturas_monrachem_20261118_205706).
 */
@Testcontainers
@DisplayName("T-049 — migración V50: tipo de asiento AUTOMATICO_DEVOLUCION_SALDO_FAVOR")
class MigracionTipoAsientoDevolucionIT {

    static final List<String> PREVIOS = List.of("AJUSTE", "APERTURA", "AUTOMATICO_COMPRA", "AUTOMATICO_GASTO",
            "AUTOMATICO_NOMINA", "AUTOMATICO_PAGO", "AUTOMATICO_VENTA", "CIERRE", "INGRESO", "MANUAL");

    static final String NUEVO = "AUTOMATICO_DEVOLUCION_SALDO_FAVOR";

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t049tipo", "t049-it-mysql-tipo-devolucion");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE asientos_contables (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "tipo ENUM('AJUSTE','APERTURA','AUTOMATICO_COMPRA','AUTOMATICO_GASTO','AUTOMATICO_NOMINA',"
                + "'AUTOMATICO_PAGO','AUTOMATICO_VENTA','CIERRE','INGRESO','MANUAL') NOT NULL, "
                + "estado ENUM('ANULADO','BORRADOR','CONTABILIZADO') NOT NULL, KEY idx_asiento_tipo (tipo))");
        for (String tipo : PREVIOS) {
            jdbc.update("INSERT INTO asientos_contables (tipo, estado) VALUES (?, 'CONTABILIZADO')", tipo);
        }
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V47__tipo_asiento_nota_credito.sql"))
                .execute(ds);
        jdbc.update("INSERT INTO asientos_contables (tipo, estado) VALUES ('AUTOMATICO_NOTA_CREDITO', 'CONTABILIZADO')");
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V50__tipo_asiento_devolucion_saldo_favor.sql")).execute(ds);
    }

    @Test
    @DisplayName("los asientos existentes de cada valor previo se conservan y el valor nuevo se puede insertar")
    void conservaLosPreviosYAceptaElNuevo() {
        for (String tipo : PREVIOS) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE tipo = ?", Integer.class,
                    tipo)).as(tipo).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE tipo = "
                + "'AUTOMATICO_NOTA_CREDITO'", Integer.class)).isEqualTo(1);
        jdbc.update("INSERT INTO asientos_contables (tipo, estado) VALUES (?, 'CONTABILIZADO')", NUEVO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE tipo = ?", Integer.class, NUEVO))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class)).isEqualTo(12);
        // los asientos previos no se modificaron
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE tipo = 'AUTOMATICO_VENTA'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("la columna sigue NOT NULL, con los doce valores y el índice idx_asiento_tipo")
    void definicionDeLaColumna() {
        Map<String, Object> col = jdbc.queryForMap("SELECT column_type, is_nullable FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'asientos_contables' AND column_name = 'tipo'");
        String tipo = String.valueOf(col.get("COLUMN_TYPE") != null ? col.get("COLUMN_TYPE") : col.get("column_type"));
        for (String v : PREVIOS) {
            assertThat(tipo).contains("'" + v + "'");
        }
        assertThat(tipo).contains("'AUTOMATICO_NOTA_CREDITO'").contains("'" + NUEVO + "'");
        // el valor nuevo va al final y el orden de los existentes no cambia
        assertThat(tipo).isEqualTo("enum('AJUSTE','APERTURA','AUTOMATICO_COMPRA','AUTOMATICO_GASTO','AUTOMATICO_NOMINA',"
                + "'AUTOMATICO_PAGO','AUTOMATICO_VENTA','CIERRE','INGRESO','MANUAL','AUTOMATICO_NOTA_CREDITO',"
                + "'" + NUEVO + "')");
        assertThat(String.valueOf(col.get("IS_NULLABLE") != null ? col.get("IS_NULLABLE") : col.get("is_nullable")))
                .isEqualTo("NO");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = "
                + "DATABASE() AND table_name = 'asientos_contables' AND index_name = 'idx_asiento_tipo'",
                Integer.class)).isEqualTo(1);
    }
}
