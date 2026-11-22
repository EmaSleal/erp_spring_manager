package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T-050 (ADR-060, P-112): corrección única de las columnas TIMESTAMP que escribe la aplicación, al pasar MySQL de UTC a
 * America/Costa_Rica.
 *
 * <p>Hasta ahora el servidor corría en UTC y la aplicación guardaba su hora local sin conversión
 * (preserveInstants=false, T-033): una columna TIMESTAMP de la aplicación contiene internamente «hora local rotulada
 * como UTC». Con el servidor en America/Costa_Rica esos valores se leerían 6 horas antes. Esta migración suma 6 horas
 * una sola vez a cada columna APP, de modo que se lea de nuevo el valor original.
 *
 * <p>Guarda: solo corre si {@code @@global.time_zone} es {@code America/Costa_Rica} o {@code -06:00} (equivalentes:
 * Costa Rica no tiene horario de verano) y la zona de la sesión es la misma (Flyway no la modifica). Si no, aborta sin
 * tocar nada: primero hay que cambiar la zona del servidor (docs/despliegue/ZONA_HORARIA_MYSQL.md). Que se ejecute una
 * sola vez lo garantiza Flyway (flyway_schema_history); la guarda impide que corra antes del cambio de zona.
 *
 * <p>No toca: las columnas BASE (las escribe la base con DEFAULT CURRENT_TIMESTAMP, ya en la zona nueva), las MIXTA (las
 * escriben la aplicación y la base; requieren decisión, solo se avisa si tienen filas) ni las DATETIME (la zona no las
 * convierte; incluye todas las fechas fiscales). Las tablas o columnas ausentes se omiten, así una base vacía pasa.
 * Todo ocurre en la transacción de Flyway.
 */
public class V51__zona_costa_rica_corregir_timestamps extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V51__zona_costa_rica_corregir_timestamps.class);

    static final List<String> ZONAS_ACEPTADAS = List.of("America/Costa_Rica", "-06:00");

    /** Columnas APP: {tabla, columna}. Clasificación y respaldo en docs/despliegue/ZONA_HORARIA_MYSQL.md. */
    static final List<String[]> COLUMNAS_APP = java.util.Collections.singletonList(
            // JdbcTokenRepositoryImpl (SecurityConfig.jdbcTokenRepository) escribe new Date() de la aplicación
            new String[] {"persistent_logins", "last_used"});

    /**
     * Columnas MIXTA: la entidad las asigna (@PrePersist/@PreUpdate con LocalDateTime.now()) y la tabla también tiene
     * DEFAULT CURRENT_TIMESTAMP / ON UPDATE. No se corrigen sin decisión; solo se avisa si tienen filas.
     */
    static final List<String[]> COLUMNAS_MIXTA = List.of(
            new String[] {"ausencias", "created_at"}, new String[] {"ausencias", "updated_at"},
            new String[] {"contratos_empleado", "created_at"}, new String[] {"contratos_empleado", "updated_at"},
            new String[] {"saldo_vacaciones", "created_at"}, new String[] {"saldo_vacaciones", "updated_at"});

    @Override
    public void migrate(Context context) throws Exception {
        aplicar(context.getConnection());
    }

    /** @return filas corregidas por «tabla.columna» (solo las columnas que existen). */
    static Map<String, Integer> aplicar(Connection con) throws SQLException {
        verificarZona(con);
        DatabaseMetaData meta = con.getMetaData();
        String catalogo = con.getCatalog();
        Map<String, Integer> corregidas = new LinkedHashMap<>();
        for (String[] c : COLUMNAS_APP) {
            String tabla = c[0];
            String columna = c[1];
            String tipo = tipoColumna(meta, catalogo, tabla, columna);
            if (tipo == null) {
                log.info("V51: no existe {}.{}; se omite", tabla, columna);
                continue;
            }
            if (!esTimestamp(tipo)) {
                log.warn("V51: {}.{} es {} y no TIMESTAMP; se omite (la zona no la convierte, sumarle 6 h la dañaría)",
                        tabla, columna, tipo);
                continue;
            }
            int filas;
            try (Statement st = con.createStatement()) {
                filas = st.executeUpdate("UPDATE " + tabla + " SET " + columna + " = " + columna
                        + " + INTERVAL 6 HOUR WHERE " + columna + " IS NOT NULL");
            }
            corregidas.put(tabla + "." + columna, filas);
            log.info("V51: {}.{} corregida (+6 h): {} fila(s)", tabla, columna, filas);
        }
        for (String[] c : COLUMNAS_MIXTA) {
            String tipo = tipoColumna(meta, catalogo, c[0], c[1]);
            if (tipo != null && !esTimestamp(tipo)) {
                log.warn("V51: columna MIXTA {}.{} es {} y no TIMESTAMP; la zona no la convierte", c[0], c[1], tipo);
            } else if (tipo != null) {
                long filas = contarNoNulas(con, c[0], c[1]);
                if (filas > 0) {
                    log.warn("V51: columna MIXTA {}.{} con {} fila(s) NO corregida: requiere decisión (ver "
                            + "docs/despliegue/ZONA_HORARIA_MYSQL.md)", c[0], c[1], filas);
                } else {
                    log.info("V51: columna MIXTA {}.{} sin filas; nada que corregir", c[0], c[1]);
                }
            }
        }
        log.info("V51: columnas TIMESTAMP corregidas: {}", corregidas);
        return corregidas;
    }

    static void verificarZona(Connection con) throws SQLException {
        String global = valor(con, "SELECT @@global.time_zone");
        String sesion = valor(con, "SELECT @@session.time_zone");
        if (!ZONAS_ACEPTADAS.contains(global) || !global.equals(sesion)) {
            throw new FlywayException("V51: el servidor MySQL no está en America/Costa_Rica (@@global.time_zone = '"
                    + global + "', @@session.time_zone = '" + sesion + "'). Esta migración suma 6 horas a los TIMESTAMP "
                    + "de la aplicación y solo es correcta después de cambiar la zona del servidor: reinicie mysql con "
                    + "--default-time-zone=America/Costa_Rica (o '-06:00' si no hay tablas de zonas con nombre), "
                    + "verifique @@global.time_zone y vuelva a arrancar la aplicación. No se modificó ninguna fila "
                    + "(docs/despliegue/ZONA_HORARIA_MYSQL.md).");
        }
    }

    private static String valor(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static long contarNoNulas(Connection con, String tabla, String columna) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE " + columna + " IS NOT NULL")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    static boolean esTimestamp(String tipo) {
        return tipo != null && tipo.toUpperCase().startsWith("TIMESTAMP");
    }

    /** TYPE_NAME de la columna en el esquema actual, o null si la tabla o la columna no existen. */
    private static String tipoColumna(DatabaseMetaData meta, String catalogo, String tabla, String columna)
            throws SQLException {
        try (ResultSet rs = meta.getColumns(catalogo, null, tabla, columna)) {
            while (rs.next()) {
                if (tabla.equalsIgnoreCase(rs.getString("TABLE_NAME"))
                        && columna.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                    return rs.getString("TYPE_NAME");
                }
            }
        }
        return null;
    }
}
