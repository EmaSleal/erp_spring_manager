package db.migration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class V41ReparacionMojibakeTest {

    private Connection con;

    @BeforeEach
    void abrir() throws Exception {
        con = DriverManager.getConnection(
                "jdbc:h2:mem:v41;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement st = con.createStatement()) {
            st.execute("DROP ALL OBJECTS");
            st.execute("CREATE TABLE articulo_maestro (id BIGINT PRIMARY KEY, descripcion_cabys VARCHAR(200), "
                    + "cantidad INT)");
            st.execute("CREATE TABLE factura (id BIGINT PRIMARY KEY, detalle VARCHAR(200))");
            st.execute("INSERT INTO articulo_maestro VALUES (1, 'poli├®ster', 5)");
            st.execute("INSERT INTO articulo_maestro VALUES (2, 'ya correcto m²', 6)");
            st.execute("INSERT INTO articulo_maestro VALUES (3, 'caña poli├®ster 中', 7)");
            st.execute("INSERT INTO articulo_maestro VALUES (4, NULL, 8)");
            st.execute("INSERT INTO factura VALUES (1, 'm┬▓')");
        }
    }

    @AfterEach
    void cerrar() throws Exception {
        con.close();
    }

    private String valor(String sql) throws Exception {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    void reparaIrreparablesYFiscalesIntactos() throws Exception {
        List<V41__reparar_mojibake_datos_maestros.Resumen> r = V41__reparar_mojibake_datos_maestros.reparar(con);
        assertEquals("poliéster", valor("SELECT descripcion_cabys FROM articulo_maestro WHERE id=1"));
        assertEquals("ya correcto m²", valor("SELECT descripcion_cabys FROM articulo_maestro WHERE id=2"));
        assertEquals("caña poli├®ster 中",
                valor("SELECT descripcion_cabys FROM articulo_maestro WHERE id=3"));
        assertEquals("m┬▓", valor("SELECT detalle FROM factura WHERE id=1"));
        assertEquals(1, r.stream().mapToInt(V41__reparar_mojibake_datos_maestros.Resumen::reparadas).sum());
        assertEquals(1, r.stream().mapToInt(V41__reparar_mojibake_datos_maestros.Resumen::irreparables).sum());
    }

    @Test
    void segundaEjecucionNoCambiaNada() throws Exception {
        V41__reparar_mojibake_datos_maestros.reparar(con);
        List<V41__reparar_mojibake_datos_maestros.Resumen> r = V41__reparar_mojibake_datos_maestros.reparar(con);
        assertEquals(0, r.stream().mapToInt(V41__reparar_mojibake_datos_maestros.Resumen::reparadas).sum());
        assertEquals("poliéster", valor("SELECT descripcion_cabys FROM articulo_maestro WHERE id=1"));
    }
}
