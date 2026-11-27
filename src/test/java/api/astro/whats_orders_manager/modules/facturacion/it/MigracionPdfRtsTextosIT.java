package api.astro.whats_orders_manager.modules.facturacion.it;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.BeforeEach;
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
 * T-059 criterio 2 (ADR-078): el SQL real de V64 sobre MySQL 8.0.32 con filas previas. Siembra el título y la leyenda en
 * la fila existente, es idempotente, no pisa un valor ya cargado (ni vuelve a sembrar una leyenda borrada), deja
 * pdf_rts_titulo VARCHAR(120) NOT NULL y pdf_rts_leyenda VARCHAR(300) NULL, y una fila nueva recibe el título de la base
 * (DEFAULT de la columna + @DynamicInsert de la entidad); la entidad pasa la validación de esquema de Hibernate (ddl=validate).
 */
@Testcontainers
@DisplayName("T-059 — migración V64: textos del PDF del régimen simplificado")
class MigracionPdfRtsTextosIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t059", "t059-it-mysql-migracion");

    static final String V64 = "db/migration/V64__pdf_rts_textos_configuracion_facturacion.sql";
    static final String TITULO = "Factura — Régimen de Tributación Simplificada";
    static final String LEYENDA = "Contribuyente del Régimen de Tributación Simplificada. No cobra IVA.";

    /** Columnas previas de configuracion_facturacion (sin las de T-059). */
    static final String TABLA = "CREATE TABLE configuracion_facturacion ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, serie_factura VARCHAR(10), numero_inicial INT, "
            + "numero_actual INT NOT NULL DEFAULT 1, prefijo_factura VARCHAR(10), moneda VARCHAR(3) NOT NULL DEFAULT 'CRC', "
            + "simbolo_moneda VARCHAR(5), terminos_condiciones TEXT, nota_pie_pagina VARCHAR(500), "
            + "incluir_igv_en_precio TINYINT(1), formato_numero VARCHAR(50), decimales INT, "
            + "activo TINYINT(1) NOT NULL DEFAULT 1, createDate DATETIME, updateDate DATETIME, createBy INT, updateBy INT)";

    static DriverManagerDataSource ds;
    static JdbcTemplate jdbc;

    @BeforeEach
    void tablaNueva() {
        ds = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS configuracion_facturacion");
        jdbc.execute(TABLA);
    }

    private static void migrar() {
        ResourceDatabasePopulator p = new ResourceDatabasePopulator(false, false, "UTF-8", new ClassPathResource(V64));
        p.execute(ds);
    }

    private static Map<String, Object> fila(int id) {
        return jdbc.queryForMap("SELECT pdf_rts_titulo, pdf_rts_leyenda FROM configuracion_facturacion WHERE id = ?", id);
    }

    private static String columna(String nombre, String atributo) {
        return jdbc.queryForObject("SELECT " + atributo + " FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'configuracion_facturacion' AND column_name = ?", String.class, nombre);
    }

    @Test
    @DisplayName("siembra el título y la leyenda en la fila existente (todas las filas con título vacío)")
    void siembra() {
        jdbc.update("INSERT INTO configuracion_facturacion (serie_factura) VALUES ('FA01'), ('FA02')");

        migrar();

        assertThat(fila(1)).containsEntry("pdf_rts_titulo", TITULO).containsEntry("pdf_rts_leyenda", LEYENDA);
        assertThat(fila(2)).containsEntry("pdf_rts_titulo", TITULO).containsEntry("pdf_rts_leyenda", LEYENDA);
    }

    @Test
    @DisplayName("tipos: pdf_rts_titulo VARCHAR(120) NOT NULL y pdf_rts_leyenda VARCHAR(300) NULL")
    void tipos() {
        jdbc.update("INSERT INTO configuracion_facturacion (serie_factura) VALUES ('FA01')");
        migrar();

        assertThat(columna("pdf_rts_titulo", "column_type")).isEqualToIgnoringCase("varchar(120)");
        assertThat(columna("pdf_rts_titulo", "is_nullable")).isEqualTo("NO");
        assertThat(columna("pdf_rts_leyenda", "column_type")).isEqualToIgnoringCase("varchar(300)");
        assertThat(columna("pdf_rts_leyenda", "is_nullable")).isEqualTo("YES");
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO configuracion_facturacion (serie_factura, pdf_rts_titulo) VALUES ('X', NULL)"));
    }

    @Test
    @DisplayName("idempotente: correrla dos veces no falla ni cambia los valores")
    void idempotente() {
        jdbc.update("INSERT INTO configuracion_facturacion (serie_factura) VALUES ('FA01')");

        migrar();
        migrar();

        assertThat(fila(1)).containsEntry("pdf_rts_titulo", TITULO).containsEntry("pdf_rts_leyenda", LEYENDA);
        assertThat(columna("pdf_rts_titulo", "column_type")).isEqualToIgnoringCase("varchar(120)");
    }

    @Test
    @DisplayName("no pisa un valor ya cargado: título editado y leyenda borrada (NULL) quedan igual al rerun")
    void noPisaLoEditado() {
        jdbc.update("INSERT INTO configuracion_facturacion (serie_factura) VALUES ('FA01')");
        migrar();
        jdbc.update("UPDATE configuracion_facturacion SET pdf_rts_titulo = 'Mi factura RTS', pdf_rts_leyenda = NULL");

        migrar();

        assertThat(fila(1)).containsEntry("pdf_rts_titulo", "Mi factura RTS").containsEntry("pdf_rts_leyenda", null);
    }

    @Test
    @DisplayName("no pisa un título que ya existía antes de la migración (columna creada a mano)")
    void tituloPreexistente() {
        jdbc.execute("ALTER TABLE configuracion_facturacion ADD COLUMN pdf_rts_titulo VARCHAR(120) NULL");
        jdbc.update("INSERT INTO configuracion_facturacion (serie_factura, pdf_rts_titulo) VALUES ('FA01', 'Factura de la casa')");
        jdbc.update("INSERT INTO configuracion_facturacion (serie_factura, pdf_rts_titulo) VALUES ('FA02', '  ')");

        migrar();

        assertThat(fila(1)).containsEntry("pdf_rts_titulo", "Factura de la casa").containsEntry("pdf_rts_leyenda", null);
        assertThat(fila(2)).containsEntry("pdf_rts_titulo", TITULO).containsEntry("pdf_rts_leyenda", LEYENDA);
    }

    @Test
    @DisplayName("tabla sin filas: la migración corre y una fila nueva recibe el título de la base (DEFAULT)")
    void sinFilasYDefault() {
        migrar();

        jdbc.update("INSERT INTO configuracion_facturacion (serie_factura) VALUES ('FA01')");
        assertThat(fila(1)).containsEntry("pdf_rts_titulo", TITULO).containsEntry("pdf_rts_leyenda", null);
    }

    @Test
    @DisplayName("la entidad coincide con la tabla (ddl=validate) y una fila nueva sin título toma el de la base (@DynamicInsert)")
    void entidadContraLaTabla() {
        migrar();

        Configuration cfg = new Configuration().addAnnotatedClass(ConfiguracionFacturacion.class)
                .setProperty("hibernate.connection.url", MYSQL.getJdbcUrl())
                .setProperty("hibernate.connection.username", MYSQL.getUsername())
                .setProperty("hibernate.connection.password", MYSQL.getPassword())
                .setProperty("hibernate.hbm2ddl.auto", "validate");
        try (SessionFactory sf = cfg.buildSessionFactory(); Session s = sf.openSession()) {
            s.beginTransaction();
            ConfiguracionFacturacion nueva = new ConfiguracionFacturacion();
            s.persist(nueva);
            s.getTransaction().commit();
            s.clear();

            ConfiguracionFacturacion leida = s.find(ConfiguracionFacturacion.class, nueva.getId());
            assertThat(leida.getPdfRtsTitulo()).isEqualTo(TITULO);
            assertThat(leida.getPdfRtsLeyenda()).isNull();
        }
    }
}
