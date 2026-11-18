package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-037 (criterio 11): V43 agrega comprobante_electronico.ambiente, lo completa con el de la configuración activa,
 * verifica que no haya consecutivos repetidos y crea el índice único. Idempotente. En H2 (modo MySQL); la corrida real
 * sobre MySQL está en el informe de la tarea.
 */
@DisplayName("T-037 — V43: restricción única del consecutivo")
class V43ConsecutivoUnicoTest {

    private Connection con;

    @BeforeEach
    void preparar() throws SQLException {
        con = DriverManager.getConnection("jdbc:h2:mem:v43-" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE configuracion_hacienda (id BIGINT PRIMARY KEY, ambiente VARCHAR(20), activa BOOLEAN)");
            st.execute("CREATE TABLE comprobante_electronico (id BIGINT AUTO_INCREMENT PRIMARY KEY, empresa_id INT, "
                + "tipo_comprobante VARCHAR(50), consecutivo VARCHAR(20))");
            st.execute("INSERT INTO configuracion_hacienda VALUES (3, 'SANDBOX', TRUE), (4, 'PRODUCCION', FALSE)");
            st.execute("INSERT INTO comprobante_electronico (empresa_id, tipo_comprobante, consecutivo) VALUES "
                + "(1, 'FACTURA_ELECTRONICA', '00000000000000000026'), (1, 'FACTURA_ELECTRONICA', '00000000000000001003'), "
                + "(1, 'NOTA_CREDITO', '00000000000000000001')");
        }
    }

    @AfterEach
    void cerrar() throws SQLException {
        con.close();
    }

    private long contar(String sql) throws SQLException {
        try (Statement st = con.createStatement(); var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    @DisplayName("agrega ambiente con el de la configuración activa (SANDBOX) y crea el índice único; la 2.ª vez no cambia nada")
    void aplicaYEsIdempotente() throws SQLException {
        V43__comprobante_consecutivo_unico.aplicar(con);
        assertThat(contar("SELECT COUNT(*) FROM comprobante_electronico WHERE ambiente = 'SANDBOX'")).isEqualTo(3);

        V43__comprobante_consecutivo_unico.aplicar(con);
        assertThat(contar("SELECT COUNT(*) FROM comprobante_electronico WHERE ambiente = 'SANDBOX'")).isEqualTo(3);

        // El índice rechaza un repetido en el mismo ambiente y admite el mismo número en otro ambiente
        try (Statement st = con.createStatement()) {
            st.execute("INSERT INTO comprobante_electronico (empresa_id, tipo_comprobante, consecutivo, ambiente) "
                + "VALUES (1, 'FACTURA_ELECTRONICA', '00000000000000000026', 'PRODUCCION')");
            assertThatThrownBy(() -> st.execute("INSERT INTO comprobante_electronico (empresa_id, tipo_comprobante, "
                + "consecutivo, ambiente) VALUES (1, 'FACTURA_ELECTRONICA', '00000000000000000026', 'SANDBOX')"))
                .isInstanceOf(SQLException.class);
        }
    }

    @Test
    @DisplayName("con consecutivos repetidos se detiene con un mensaje que los lista y no crea el índice")
    void conRepetidos_seDetiene() throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("INSERT INTO comprobante_electronico (empresa_id, tipo_comprobante, consecutivo) VALUES "
                + "(1, 'FACTURA_ELECTRONICA', '00000000000000000026')");
        }
        assertThatThrownBy(() -> V43__comprobante_consecutivo_unico.aplicar(con))
            .isInstanceOf(FlywayException.class)
            .hasMessageContaining("consecutivo 00000000000000000026 (2 filas)")
            .hasMessageContaining("Emanuel");
        // Sin índice: el repetido sigue insertándose
        try (Statement st = con.createStatement()) {
            st.execute("INSERT INTO comprobante_electronico (empresa_id, tipo_comprobante, consecutivo, ambiente) VALUES "
                + "(1, 'FACTURA_ELECTRONICA', '00000000000000000026', 'SANDBOX')");
        }
    }
}
