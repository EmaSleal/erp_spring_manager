package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * T-037 (ADR-043 §6, P-089): restricción única del consecutivo en comprobante_electronico como última defensa contra
 * un número repetido.
 *
 * <p>La numeración es propia de cada configuración de Hacienda (SANDBOX y PRODUCCION cuentan por separado), y la tabla
 * no guardaba el ambiente. Esta migración:
 * <ol>
 *   <li>agrega la columna {@code ambiente} (VARCHAR(20), el nombre de AmbienteHacienda) si no existe;</li>
 *   <li>completa las filas sin ambiente con el de la configuración de Hacienda activa (ADR-021: hay una sola). Todos los
 *       comprobantes existentes se emitieron con ella;</li>
 *   <li>verifica que no haya consecutivos repetidos por (empresa, ambiente, tipo). Si los hay, se detiene con un
 *       mensaje que los lista: no crea la restricción ni falla en silencio; la decisión es de Emanuel;</li>
 *   <li>crea el índice único {@code uk_comprobante_consecutivo (empresa_id, ambiente, tipo_comprobante, consecutivo)}
 *       si no existe.</li>
 * </ol>
 * Idempotente: una segunda ejecución no cambia nada.
 */
public class V43__comprobante_consecutivo_unico extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V43__comprobante_consecutivo_unico.class);

    static final String TABLA = "comprobante_electronico";
    static final String INDICE = "uk_comprobante_consecutivo";

    @Override
    public void migrate(Context context) throws Exception {
        aplicar(context.getConnection());
    }

    static void aplicar(Connection con) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        String catalogo = con.getCatalog();
        if (!existeTabla(meta, catalogo, TABLA)) {
            log.info("V43: no existe {}; nada que hacer", TABLA);
            return;
        }
        if (!existeColumna(meta, catalogo, TABLA, "ambiente")) {
            try (Statement st = con.createStatement()) {
                st.execute("ALTER TABLE " + TABLA + " ADD COLUMN ambiente VARCHAR(20) NULL");
            }
            log.info("V43: columna {}.ambiente agregada", TABLA);
        }

        String ambienteActivo = ambienteDeLaConfiguracionActiva(con, meta, catalogo);
        if (ambienteActivo != null) {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE " + TABLA + " SET ambiente = ? WHERE ambiente IS NULL")) {
                ps.setString(1, ambienteActivo);
                int filas = ps.executeUpdate();
                if (filas > 0) {
                    log.info("V43: {} comprobante(s) sin ambiente quedan con {}", filas, ambienteActivo);
                }
            }
        }

        List<String> duplicados = duplicados(con);
        if (!duplicados.isEmpty()) {
            throw new FlywayException("V43: hay consecutivos repetidos en " + TABLA + " por (empresa, ambiente, tipo, "
                + "consecutivo), así que no se crea la restricción única " + INDICE + ". Repetidos: "
                + String.join("; ", duplicados) + ". Revíselos con Emanuel antes de volver a desplegar (P-089).");
        }

        if (!existeIndice(meta, catalogo, TABLA, INDICE)) {
            try (Statement st = con.createStatement()) {
                st.execute("CREATE UNIQUE INDEX " + INDICE + " ON " + TABLA
                    + " (empresa_id, ambiente, tipo_comprobante, consecutivo)");
            }
            log.info("V43: índice único {} creado", INDICE);
        }
    }

    static List<String> duplicados(Connection con) throws SQLException {
        List<String> repetidos = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT empresa_id, ambiente, tipo_comprobante, consecutivo, COUNT(*) "
                 + "FROM " + TABLA + " GROUP BY empresa_id, ambiente, tipo_comprobante, consecutivo "
                 + "HAVING COUNT(*) > 1")) {
            while (rs.next()) {
                repetidos.add("empresa " + rs.getObject(1) + ", " + rs.getString(2) + ", " + rs.getString(3)
                    + ", consecutivo " + rs.getString(4) + " (" + rs.getLong(5) + " filas)");
            }
        }
        return repetidos;
    }

    private static String ambienteDeLaConfiguracionActiva(Connection con, DatabaseMetaData meta, String catalogo)
            throws SQLException {
        if (!existeTabla(meta, catalogo, "configuracion_hacienda")) {
            return null;
        }
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT ambiente FROM configuracion_hacienda WHERE activa = TRUE ORDER BY id")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static boolean existeTabla(DatabaseMetaData meta, String catalogo, String tabla) throws SQLException {
        try (ResultSet rs = meta.getTables(catalogo, null, "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                if (tabla.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean existeColumna(DatabaseMetaData meta, String catalogo, String tabla, String columna)
            throws SQLException {
        try (ResultSet rs = meta.getColumns(catalogo, null, "%", "%")) {
            while (rs.next()) {
                if (tabla.equalsIgnoreCase(rs.getString("TABLE_NAME"))
                        && columna.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean existeIndice(DatabaseMetaData meta, String catalogo, String tabla, String indice)
            throws SQLException {
        for (String nombre : List.of(tabla, tabla.toUpperCase())) {
            try (ResultSet rs = meta.getIndexInfo(catalogo, null, nombre, false, false)) {
                while (rs.next()) {
                    if (indice.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
