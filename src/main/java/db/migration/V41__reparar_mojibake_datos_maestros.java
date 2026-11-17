package db.migration;

import api.astro.whats_orders_manager.shared.util.ReparadorMojibake;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * T-035: repara texto con doble codificacion (UTF-8 leido como IBM850/IBM437/Windows-1252) en datos maestros
 * y catalogos. No toca tablas fiscales ni historicas. Idempotente: solo actualiza filas con marcadores
 * cuya reparacion es UTF-8 valido sin marcadores; las irreparables quedan igual y se informan.
 */
public class V41__reparar_mojibake_datos_maestros extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V41__reparar_mojibake_datos_maestros.class);

    /** Lista blanca de tablas (datos maestros y catalogos). Las columnas de texto se descubren por metadatos. */
    static final List<String> TABLAS = List.of(
            "articulo_maestro", "producto", "presentacion", "cliente", "proveedor", "proveedores",
            "configuracion_empresa", "empresa",
            "parametro_sistema", "permiso", "plantilla_whatsapp", "plantilla_notificacion", "rol");

    /** Columnas que nunca se tocan aunque la tabla este en la lista (secretos). */
    private static final List<String> EXCLUIDAS = List.of("password", "pin", "token", "secret", "clave");

    @Override
    public void migrate(Context context) throws Exception {
        reparar(context.getConnection());
    }

    /** Resultado por columna. */
    public record Resumen(String tabla, String columna, int reparadas, int irreparables) {
    }

    static List<Resumen> reparar(Connection con) throws SQLException {
        List<Resumen> resumenes = new ArrayList<>();
        DatabaseMetaData meta = con.getMetaData();
        String catalogo = con.getCatalog();
        for (String tabla : TABLAS) {
            String nombreReal = nombreTabla(meta, catalogo, tabla);
            if (nombreReal == null) {
                continue;
            }
            String pk = clavePrimariaSimple(meta, catalogo, nombreReal);
            if (pk == null) {
                log.warn("V41: tabla {} sin clave primaria simple, se omite", nombreReal);
                continue;
            }
            for (String columna : columnasTexto(meta, catalogo, nombreReal, pk)) {
                resumenes.add(repararColumna(con, nombreReal, pk, columna));
            }
        }
        int total = resumenes.stream().mapToInt(Resumen::reparadas).sum();
        log.info("V41: mojibake reparado en {} valores ({} columnas revisadas)", total, resumenes.size());
        return resumenes;
    }

    private static String nombreTabla(DatabaseMetaData meta, String catalogo, String tabla) throws SQLException {
        try (ResultSet rs = meta.getTables(catalogo, null, "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                String n = rs.getString("TABLE_NAME");
                if (n != null && n.equalsIgnoreCase(tabla)) {
                    return n;
                }
            }
        }
        return null;
    }

    private static String clavePrimariaSimple(DatabaseMetaData meta, String catalogo, String tabla)
            throws SQLException {
        List<String> pks = new ArrayList<>();
        try (ResultSet rs = meta.getPrimaryKeys(catalogo, null, tabla)) {
            while (rs.next()) {
                pks.add(rs.getString("COLUMN_NAME"));
            }
        }
        return pks.size() == 1 ? pks.get(0) : null;
    }

    private static List<String> columnasTexto(DatabaseMetaData meta, String catalogo, String tabla, String pk)
            throws SQLException {
        List<String> cols = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(catalogo, null, tabla, "%")) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                String tipo = rs.getString("TYPE_NAME").toUpperCase(Locale.ROOT);
                boolean texto = (tipo.contains("CHAR") || tipo.contains("TEXT") || tipo.contains("CLOB"))
                        && !tipo.contains("BINARY");
                String lower = nombre.toLowerCase(Locale.ROOT);
                boolean secreta = EXCLUIDAS.stream().anyMatch(lower::contains);
                if (texto && !secreta && !nombre.equalsIgnoreCase(pk)) {
                    cols.add(nombre);
                }
            }
        }
        return cols;
    }

    private static Resumen repararColumna(Connection con, String tabla, String pk, String columna)
            throws SQLException {
        String select = "SELECT `" + pk + "`, `" + columna + "` FROM `" + tabla + "` WHERE `" + columna
                + "` IS NOT NULL";
        String update = "UPDATE `" + tabla + "` SET `" + columna + "` = ? WHERE `" + pk + "` = ?";
        List<Object[]> cambios = new ArrayList<>();
        int irreparables = 0;
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(select)) {
            while (rs.next()) {
                String valor = rs.getString(2);
                if (!ReparadorMojibake.tieneMarcadores(valor)) {
                    continue;
                }
                Optional<String> r = ReparadorMojibake.reparar(valor);
                if (r.isPresent()) {
                    cambios.add(new Object[] {r.get(), rs.getObject(1)});
                } else {
                    irreparables++;
                    String ejemplo = valor.length() > 60 ? valor.substring(0, 60) : valor;
                    log.warn("V41: irreparable {}.{} pk={} valor='{}'", tabla, columna, rs.getObject(1), ejemplo);
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(update)) {
            for (Object[] c : cambios) {
                ps.setString(1, (String) c[0]);
                ps.setObject(2, c[1]);
                ps.addBatch();
            }
            if (!cambios.isEmpty()) {
                ps.executeBatch();
            }
        }
        if (!cambios.isEmpty() || irreparables > 0) {
            log.info("V41: {}.{} reparadas={} irreparables={}", tabla, columna, cambios.size(), irreparables);
        }
        return new Resumen(tabla, columna, cambios.size(), irreparables);
    }
}
