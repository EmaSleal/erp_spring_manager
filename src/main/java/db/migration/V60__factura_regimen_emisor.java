package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * T-057 (ADR-073 Opción A, ADR-076; revisión 1): régimen tributario sellado en la factura. {@code regimen_emisor} se fija
 * al crear la factura con el régimen vigente en su fecha (RegimenTributarioService.vigenteEn) y no se recalcula nunca.
 *
 * <p>Las facturas existentes se rellenan con la misma regla que el servicio y la misma fecha que usa el sello al crear:
 * el régimen del tramo de {@code regimen_tributario} con el mayor {@code vigente_desde} menor o igual a
 * {@code DATE(factura.create_date)} (DATETIME en America/Costa_Rica desde V51, así que DATE() es la fecha de Costa Rica).
 * No hay ids, fechas ni regímenes literales: todo sale de la tabla. Así las facturas de julio de 2026, calculadas sin IVA
 * dentro del tramo histórico SIMPLIFICADO, quedan SIMPLIFICADO y una edición no les agrega IVA.
 *
 * <p>Es una migración Java (como V51 y V54) porque SQL no puede abortar con un mensaje legible sin rutinas (ADR-028): si
 * alguna factura no tiene tramo para su fecha (o no tiene fecha de creación) se aborta nombrando cuántas y cuáles, sin
 * tocar el esquema, en lugar de suponer un régimen. Es idempotente: si la columna ya existe solo rellena las filas nulas y
 * completa la restricción NOT NULL y el CHECK que falten. No deja valor por defecto: toda factura nueva trae su régimen.
 */
public class V60__factura_regimen_emisor extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V60__factura_regimen_emisor.class);

    static final String CHECK = "ck_factura_regimen_emisor";

    private static final String SIN_TRAMO = "FROM factura f WHERE NOT EXISTS (SELECT 1 FROM regimen_tributario r "
            + "WHERE r.vigente_desde <= DATE(f.create_date))";

    @Override
    public void migrate(Context context) throws Exception {
        aplicar(context.getConnection());
    }

    public static void aplicar(Connection con) throws SQLException {
        verificarTramos(con);
        try (Statement st = con.createStatement()) {
            if (!existeColumna(con)) {
                st.execute("ALTER TABLE factura ADD COLUMN regimen_emisor VARCHAR(12) NULL");
            }
            int filas = st.executeUpdate("UPDATE factura f SET f.regimen_emisor = (SELECT r.regimen FROM regimen_tributario r "
                    + "WHERE r.vigente_desde <= DATE(f.create_date) ORDER BY r.vigente_desde DESC LIMIT 1) "
                    + "WHERE f.regimen_emisor IS NULL");
            log.info("V60: regimen_emisor rellenado según regimen_tributario en {} factura(s)", filas);
            st.execute("ALTER TABLE factura MODIFY COLUMN regimen_emisor VARCHAR(12) NOT NULL");
            if (!existeCheck(con)) {
                st.execute("ALTER TABLE factura ADD CONSTRAINT " + CHECK
                        + " CHECK (regimen_emisor IN ('GENERAL', 'SIMPLIFICADO'))");
            }
        }
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT regimen_emisor, COUNT(*) FROM factura GROUP BY regimen_emisor")) {
            while (rs.next()) {
                log.info("V60: {} factura(s) con regimen_emisor = {}", rs.getLong(2), rs.getString(1));
            }
        }
    }

    /** Aborta antes de tocar el esquema si alguna factura sin sello no tiene tramo para su fecha. */
    private static void verificarTramos(Connection con) throws SQLException {
        boolean columna = existeColumna(con);
        String filtro = columna ? " AND f.regimen_emisor IS NULL" : "";
        long sinTramo;
        List<String> ejemplos = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) " + SIN_TRAMO + filtro)) {
                rs.next();
                sinTramo = rs.getLong(1);
            }
            if (sinTramo == 0) {
                return;
            }
            try (ResultSet rs = st.executeQuery("SELECT f.id_factura, f.create_date " + SIN_TRAMO + filtro
                    + " ORDER BY f.id_factura LIMIT 10")) {
                while (rs.next()) {
                    ejemplos.add(rs.getLong(1) + " (" + rs.getString(2) + ")");
                }
            }
        }
        throw new FlywayException("V60 abortada: " + sinTramo + " factura(s) sin tramo de regimen_tributario para su fecha de "
                + "creación (o sin fecha), por ejemplo id_factura " + ejemplos + ". No se supone un régimen: registre el "
                + "tramo que cubra esas fechas (o corrija su fecha) y vuelva a arrancar. No se modificó el esquema ni ninguna fila.");
    }

    private static boolean existeColumna(Connection con) throws SQLException {
        return contar(con, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'factura' AND column_name = 'regimen_emisor'") > 0;
    }

    private static boolean existeCheck(Connection con) throws SQLException {
        return contar(con, "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() "
                + "AND table_name = 'factura' AND constraint_name = '" + CHECK + "'") > 0;
    }

    private static long contar(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
