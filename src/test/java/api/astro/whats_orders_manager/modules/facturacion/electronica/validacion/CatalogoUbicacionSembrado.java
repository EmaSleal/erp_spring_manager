package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las pruebas de integración crean el esquema con Hibernate (sin Flyway) y los catálogos de ubicación no se siembran
 * con migraciones: esto agrega San José (provincia 1, cantón 01, distrito 01) y Alajuela (2, 01, 04), idempotente.
 */
public final class CatalogoUbicacionSembrado {

    private CatalogoUbicacionSembrado() {
    }

    public static void sembrar(JdbcTemplate jdbc) {
        jdbc.update("INSERT IGNORE INTO cat_provincia_cr (codigo, nombre) VALUES ('1', 'San José'), ('2', 'Alajuela')");
        jdbc.update("INSERT IGNORE INTO cat_canton_cr (id, provincia_codigo, codigo, nombre) VALUES "
            + "(9001, '1', '01', 'San José'), (9002, '2', '01', 'Alajuela')");
        jdbc.update("INSERT IGNORE INTO cat_distrito_cr (id, provincia_codigo, canton_codigo, codigo, nombre) VALUES "
            + "(9001, '1', '01', '01', 'Carmen'), (9002, '2', '01', '04', 'San Rafael')");
    }
}
