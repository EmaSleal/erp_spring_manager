package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * T-054 (ADR-067 §2, criterio 1): crea las cuentas del sistema de inventario periódico y configura la cuenta de compras
 * de mercadería.
 *
 * <pre>
 * 1.1.04      Inventarios                                      grupo  ACTIVO  DEUDORA   (padre 1.1)
 * 1.1.04.001  Inventario de mercadería                         hoja   ACTIVO  DEUDORA
 * 5.3         Costo de ventas                                  grupo  EGRESO  DEUDORA   (padre 5)
 * 5.3.01      Compras                                          grupo  EGRESO  DEUDORA
 * 5.3.01.001  Compras de mercadería                            hoja   EGRESO  DEUDORA
 * 5.3.01.002  Fletes y gastos de importación s/compras         hoja   EGRESO  DEUDORA
 * 5.3.01.003  Devoluciones y descuentos s/compras              hoja   EGRESO  ACREEDORA
 * 5.3.02      Costo de ventas                                  grupo  EGRESO  DEUDORA
 * 5.3.02.001  Costo de ventas                                  hoja   EGRESO  DEUDORA
 * </pre>
 *
 * y la clave {@code COMPRAS_MERCADERIA} de parametros_contables apuntando a 5.3.01.001 (la cuenta definida por el
 * contador; T-053 la propone en las líneas de mercadería). No toca {@code CPP_CUENTA_COMPRAS}.
 *
 * <p>Verificación (SQL no puede abortar con un mensaje legible sin rutinas, ADR-028): por cada código se busca la cuenta
 * existente. Libre → se crea. Existe y coincide exactamente (nombre, tipo, naturaleza, si acepta movimientos y código
 * del padre) → ya aplicada, se conserva. Existe con cualquier otra cosa → se aborta nombrando el código y la cuenta
 * ocupante, sin crear nada. También aborta si faltan las cuentas padre «1.1» o «5». Es idempotente. Todo ocurre en la
 * transacción de Flyway.
 */
public class V54__cuentas_inventario_costo_ventas extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V54__cuentas_inventario_costo_ventas.class);

    static final String CLAVE_COMPRAS_MERCADERIA = "COMPRAS_MERCADERIA";
    static final String DESCRIPCION_PARAMETRO = "Compras de mercadería (sistema periódico)";
    static final String CODIGO_COMPRAS_MERCADERIA = "5.3.01.001";

    /** Cuenta que crea la migración; padre = código de la cuenta padre (existente o creada antes en la lista). */
    record Cuenta(String codigo, String nombre, String tipo, String naturaleza, boolean hoja, String padre) {
        int nivel() {
            return codigo.split("\\.").length;
        }
    }

    static final List<Cuenta> CUENTAS = List.of(
            new Cuenta("1.1.04", "Inventarios", "ACTIVO", "DEUDORA", false, "1.1"),
            new Cuenta("1.1.04.001", "Inventario de mercadería", "ACTIVO", "DEUDORA", true, "1.1.04"),
            new Cuenta("5.3", "Costo de ventas", "EGRESO", "DEUDORA", false, "5"),
            new Cuenta("5.3.01", "Compras", "EGRESO", "DEUDORA", false, "5.3"),
            new Cuenta("5.3.01.001", "Compras de mercadería", "EGRESO", "DEUDORA", true, "5.3.01"),
            new Cuenta("5.3.01.002", "Fletes y gastos de importación s/compras", "EGRESO", "DEUDORA", true, "5.3.01"),
            new Cuenta("5.3.01.003", "Devoluciones y descuentos s/compras", "EGRESO", "ACREEDORA", true, "5.3.01"),
            new Cuenta("5.3.02", "Costo de ventas", "EGRESO", "DEUDORA", false, "5.3"),
            new Cuenta("5.3.02.001", "Costo de ventas", "EGRESO", "DEUDORA", true, "5.3.02"));

    @Override
    public void migrate(Context context) throws Exception {
        aplicar(context.getConnection());
    }

    /** Fila existente de cuentas_contables. */
    private record Existente(long id, String nombre, String tipo, String naturaleza, boolean acepta, String padre) {}

    static void aplicar(Connection con) throws SQLException {
        // 1. Verificar todo antes de escribir: ninguna cuenta ocupada por otra y existen los padres
        List<String> conflictos = new ArrayList<>();
        List<Cuenta> porCrear = new ArrayList<>();
        for (Cuenta c : CUENTAS) {
            Existente e = buscar(con, c.codigo());
            if (e == null) {
                porCrear.add(c);
            } else if (!coincide(c, e)) {
                conflictos.add(String.format("%s ya existe como «%s» (%s, %s, %s, padre %s) y esta migración crea «%s» (%s, %s, %s, padre %s)",
                        c.codigo(), e.nombre(), e.tipo(), e.naturaleza(), e.acepta() ? "hoja" : "grupo", e.padre(),
                        c.nombre(), c.tipo(), c.naturaleza(), c.hoja() ? "hoja" : "grupo", c.padre()));
            }
        }
        if (!conflictos.isEmpty()) {
            throw new FlywayException("V54 abortada: los códigos de cuenta que debe crear están ocupados por otras cuentas. "
                    + "Libere o renombre esas cuentas y vuelva a migrar. Detalle: " + String.join("; ", conflictos));
        }
        for (String padre : List.of("1.1", "5")) {
            if (buscar(con, padre) == null) {
                throw new FlywayException("V54 abortada: falta la cuenta padre «" + padre + "» del plan de cuentas");
            }
        }

        // 2. Crear lo que falta (los padres van antes en la lista)
        for (Cuenta c : porCrear) {
            long padreId = buscar(con, c.padre()).id();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO cuentas_contables (acepta_movimientos, activa, codigo, descripcion, naturaleza, "
                            + "nivel, nombre, tipo, cuenta_padre_id) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setBoolean(1, c.hoja());
                ps.setString(2, c.codigo());
                ps.setString(3, "Sistema de inventario periódico (ADR-067)");
                ps.setString(4, c.naturaleza());
                ps.setInt(5, c.nivel());
                ps.setString(6, c.nombre());
                ps.setString(7, c.tipo());
                ps.setLong(8, padreId);
                ps.executeUpdate();
            }
            log.info("V54: cuenta {} «{}» creada", c.codigo(), c.nombre());
        }

        // 3. COMPRAS_MERCADERIA → 5.3.01.001 (inserta o actualiza)
        long cuentaCompras = buscar(con, CODIGO_COMPRAS_MERCADERIA).id();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE cuenta_contable_id = VALUES(cuenta_contable_id)")) {
            ps.setString(1, CLAVE_COMPRAS_MERCADERIA);
            ps.setString(2, DESCRIPCION_PARAMETRO);
            ps.setLong(3, cuentaCompras);
            ps.executeUpdate();
        }
    }

    private static boolean coincide(Cuenta c, Existente e) {
        return c.nombre().equals(e.nombre()) && c.tipo().equals(e.tipo()) && c.naturaleza().equals(e.naturaleza())
                && c.hoja() == e.acepta() && c.padre().equals(e.padre());
    }

    private static Existente buscar(Connection con, String codigo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT c.id, c.nombre, c.tipo, c.naturaleza, c.acepta_movimientos, p.codigo FROM cuentas_contables c "
                        + "LEFT JOIN cuentas_contables p ON p.id = c.cuenta_padre_id WHERE c.codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Existente(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBoolean(5), rs.getString(6)) : null;
            }
        }
    }
}
