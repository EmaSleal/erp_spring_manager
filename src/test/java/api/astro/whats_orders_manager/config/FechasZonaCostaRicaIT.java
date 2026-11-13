package api.astro.whats_orders_manager.config;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-033 (Review 1, criterio 26): con la JVM en America/Costa_Rica (TZ del contenedor) y la URL con
 * serverTimezone=UTC (la de producción), las fechas y horas se leen y escriben tal como están en la columna. Sin
 * MysqlFechasSinConversionConfig, una factura creada a las 00:20 se leía como las 18:20 del día anterior y cambiaba
 * su fecha de emisión (verificado sobre el dump de producción).
 *
 * Base: MySQL 8 en Testcontainers (t033-it-mysql, tmpfs, sin volúmenes); esquema de Hibernate.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@Testcontainers
@DisplayName("T-033 — fechas sin desplazamiento con la JVM en hora de Costa Rica y la conexión en UTC (MySQL)")
class FechasZonaCostaRicaIT {

    private static TimeZone zonaOriginal;

    /** Como el contenedor de producción tras T-033 (TZ=America/Costa_Rica), antes de arrancar el contexto. */
    @BeforeAll
    static void zonaCostaRica() {
        zonaOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Costa_Rica"));
    }

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t033")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t033-it-mysql"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("database/routines/stored_procedures.sql")),
                    "/docker-entrypoint-initdb.d/01-routines.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
                + (MYSQL.getJdbcUrl().contains("?") ? "&" : "?") + "serverTimezone=UTC");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @AfterAll
    static void restaurarZona() {
        TimeZone.setDefault(zonaOriginal);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;

    @Test
    @DisplayName("DATETIME 00:20 guardado se lee 00:20 del mismo día: la fecha de emisión no cambia")
    void lectura_datetimeSinDesplazamiento() {
        int id = factura();
        jdbc.update("UPDATE factura SET create_date = '2026-07-04 00:20:00' WHERE id_factura = ?", id);

        Factura leida = new TransactionTemplate(transactionManager)
                .execute(s -> facturaRepository.findById(id).orElseThrow());

        assertThat(TimeZone.getDefault().getID()).isEqualTo("America/Costa_Rica");
        assertThat(leida.getCreateDate()).isEqualTo(LocalDateTime.of(2026, 7, 4, 0, 20));
        assertThat(leida.getFechaEmision()).isEqualTo(LocalDate.of(2026, 7, 4));
    }

    @Test
    @DisplayName("la app guarda 21:22:23 (hora de Costa Rica) y la columna queda 21:22:23")
    void escritura_datetimeSinDesplazamiento() {
        int id = factura();
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            Factura f = facturaRepository.findById(id).orElseThrow();
            f.setUpdateDate(LocalDateTime.of(2026, 11, 12, 21, 22, 23));
            facturaRepository.save(f);
        });

        assertThat(jdbc.queryForObject("SELECT DATE_FORMAT(update_date, '%Y-%m-%d %H:%i:%s') FROM factura "
                + "WHERE id_factura = ?", String.class, id)).isEqualTo("2026-11-12 21:22:23");
    }

    @Test
    @DisplayName("consultas nativas: el Timestamp del conector trae la hora guardada")
    void nativa_timestampSinDesplazamiento() {
        int id = factura();
        jdbc.update("UPDATE factura SET create_date = '2026-07-04 03:00:00' WHERE id_factura = ?", id);

        Timestamp ts = jdbc.queryForObject("SELECT create_date FROM factura WHERE id_factura = ?", Timestamp.class, id);

        assertThat(ts.toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 7, 4, 3, 0));
    }

    @Test
    @DisplayName("columnas DATE (java.sql.Date): se leen y escriben con el mismo día")
    void date_sinDesplazamiento() {
        int id = factura();
        jdbc.update("UPDATE factura SET fecha_entrega = '2026-07-04' WHERE id_factura = ?", id);
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            Factura f = facturaRepository.findById(id).orElseThrow();
            assertThat(f.getFechaEntrega().toLocalDate()).isEqualTo(LocalDate.of(2026, 7, 4));
            f.setFechaVencimiento(Date.valueOf(LocalDate.of(2026, 8, 15)));
            facturaRepository.save(f);
        });

        assertThat(jdbc.queryForObject("SELECT DATE_FORMAT(fecha_vencimiento, '%Y-%m-%d') FROM factura "
                + "WHERE id_factura = ?", String.class, id)).isEqualTo("2026-08-15");
    }

    private int factura() {
        return new TransactionTemplate(transactionManager).execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T033 " + UUID.randomUUID());
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T033-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
    }
}
