package db.migration;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-054 (ADR-067 §2, criterio 1): V54 crea 1.1.04 «Inventarios» y 5.3 «Costo de ventas» con sus hojas sobre MySQL 8.0.32,
 * en tablas con el esquema real de cuentas_contables y parametros_contables y filas como las del dump de producción
 * (ids 26 «5», 27 «5.1», 5 «5.1.01», 32 «1», 33 «1.1»). Verifica: crea el árbol con padre y nivel correctos, configura
 * COMPRAS_MERCADERIA, es idempotente, completa un estado parcial, no toca otras cuentas y aborta con un mensaje claro
 * (sin crear nada) si algún código está ocupado por otra cuenta o falta un padre.
 *
 * Base: MySQL en Testcontainers (t054-it-mysql-v54), tmpfs.
 */
@Testcontainers
@DisplayName("T-054 — migración V54: cuentas de inventario y costo de ventas")
class V54CuentasInventarioCostoVentasIT {

    @Container
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedor("t054v54", "t054-it-mysql-v54");

    private Connection con;

    @BeforeEach
    void sembrar() throws SQLException {
        if (con != null) {
            con.close();
        }
        con = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        try (Statement st = con.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            st.execute("DROP TABLE IF EXISTS parametros_contables");
            st.execute("DROP TABLE IF EXISTS cuentas_contables");
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
            st.execute("CREATE TABLE cuentas_contables (id bigint NOT NULL AUTO_INCREMENT, acepta_movimientos bit(1) NOT NULL, "
                    + "activa bit(1) NOT NULL, codigo varchar(20) NOT NULL, creado_por int DEFAULT NULL, "
                    + "descripcion varchar(500) DEFAULT NULL, fecha_creacion datetime(6) DEFAULT NULL, "
                    + "fecha_modificacion datetime(6) DEFAULT NULL, modificado_por int DEFAULT NULL, "
                    + "naturaleza enum('ACREEDORA','DEUDORA') NOT NULL, nivel int NOT NULL, nombre varchar(200) NOT NULL, "
                    + "tipo enum('ACTIVO','CAPITAL','EGRESO','INGRESO','PASIVO') NOT NULL, cuenta_padre_id bigint DEFAULT NULL, "
                    + "PRIMARY KEY (id), UNIQUE KEY idx_cuenta_codigo (codigo), "
                    + "CONSTRAINT fk_padre FOREIGN KEY (cuenta_padre_id) REFERENCES cuentas_contables (id))");
            st.execute("CREATE TABLE parametros_contables (id bigint NOT NULL AUTO_INCREMENT, clave varchar(100) NOT NULL, "
                    + "descripcion varchar(300) DEFAULT NULL, cuenta_contable_id bigint NOT NULL, PRIMARY KEY (id), "
                    + "UNIQUE KEY idx_param_clave (clave), CONSTRAINT fk_param FOREIGN KEY (cuenta_contable_id) "
                    + "REFERENCES cuentas_contables (id))");
            cuenta(st, 32, "1", "Activo", "ACTIVO", "DEUDORA", 1, "NULL", false);
            cuenta(st, 33, "1.1", "Activo Corriente", "ACTIVO", "DEUDORA", 2, "32", false);
            cuenta(st, 3, "1.1.03", "IVA Crédito Fiscal", "ACTIVO", "DEUDORA", 3, "33", true);
            cuenta(st, 26, "5", "Gastos", "EGRESO", "DEUDORA", 1, "NULL", false);
            cuenta(st, 27, "5.1", "Gastos de Operación", "EGRESO", "DEUDORA", 2, "26", false);
            cuenta(st, 5, "5.1.01", "Compras", "EGRESO", "DEUDORA", 3, "27", true);
            st.execute("INSERT INTO parametros_contables (id, clave, descripcion, cuenta_contable_id) VALUES "
                    + "(1, 'CPP_CUENTA_COMPRAS', 'Débito compras a proveedores', 5)");
        }
    }

    private static void cuenta(Statement st, long id, String codigo, String nombre, String tipo, String naturaleza, int nivel,
                               String padre, boolean acepta) throws SQLException {
        st.execute("INSERT INTO cuentas_contables (id, acepta_movimientos, activa, codigo, naturaleza, nivel, nombre, tipo, "
                + "cuenta_padre_id) VALUES (" + id + ", " + (acepta ? "b'1'" : "b'0'") + ", b'1', '" + codigo + "', '"
                + naturaleza + "', " + nivel + ", '" + nombre + "', '" + tipo + "', " + padre + ")");
    }

    private String texto(String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private long contar(String sql) throws SQLException {
        return Long.parseLong(texto(sql));
    }

    private List<String> arbol(String prefijo) throws SQLException {
        List<String> lineas = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT c.codigo, c.nombre, c.tipo, c.naturaleza, c.nivel, c.acepta_movimientos, "
                     + "p.codigo AS padre FROM cuentas_contables c LEFT JOIN cuentas_contables p ON p.id = c.cuenta_padre_id "
                     + "WHERE c.codigo LIKE '" + prefijo + "%' ORDER BY c.codigo")) {
            while (rs.next()) {
                lineas.add("  ".repeat(rs.getInt("nivel") - 1) + rs.getString("codigo") + " " + rs.getString("nombre")
                        + " [" + rs.getString("tipo") + " " + rs.getString("naturaleza")
                        + (rs.getBoolean("acepta_movimientos") ? " hoja" : " grupo") + " padre="
                        + rs.getString("padre") + "]");
            }
        }
        return lineas;
    }

    @Test
    @DisplayName("crea el árbol 1.1.04 y 5.3 con padre, nivel, tipo y naturaleza correctos; no toca otras cuentas; configura COMPRAS_MERCADERIA")
    void creaElArbol() throws Exception {
        long antes = contar("SELECT COUNT(*) FROM cuentas_contables");
        String huellaAntes = texto("SELECT GROUP_CONCAT(CONCAT_WS('|', id, codigo, nombre, tipo, naturaleza, nivel, cuenta_padre_id, "
                + "acepta_movimientos) ORDER BY id) FROM cuentas_contables");

        V54__cuentas_inventario_costo_ventas.aplicar(con);

        assertThat(contar("SELECT COUNT(*) FROM cuentas_contables")).isEqualTo(antes + 9);
        assertThat(texto("SELECT GROUP_CONCAT(CONCAT_WS('|', id, codigo, nombre, tipo, naturaleza, nivel, cuenta_padre_id, "
                + "acepta_movimientos) ORDER BY id) FROM cuentas_contables WHERE id <= 33"))
                .as("las cuentas existentes quedan idénticas").isEqualTo(huellaAntes);

        List<String> g1 = arbol("1.1");
        List<String> g5 = arbol("5");
        System.out.println("T-054 árbol grupo 1.1:\n" + String.join("\n", g1));
        System.out.println("T-054 árbol grupo 5:\n" + String.join("\n", g5));
        assertThat(g1).contains(
                "    1.1.04 Inventarios [ACTIVO DEUDORA grupo padre=1.1]",
                "      1.1.04.001 Inventario de mercadería [ACTIVO DEUDORA hoja padre=1.1.04]");
        assertThat(g5).contains(
                "  5.3 Costo de ventas [EGRESO DEUDORA grupo padre=5]",
                "    5.3.01 Compras [EGRESO DEUDORA grupo padre=5.3]",
                "      5.3.01.001 Compras de mercadería [EGRESO DEUDORA hoja padre=5.3.01]",
                "      5.3.01.002 Fletes y gastos de importación s/compras [EGRESO DEUDORA hoja padre=5.3.01]",
                "      5.3.01.003 Devoluciones y descuentos s/compras [EGRESO ACREEDORA hoja padre=5.3.01]",
                "    5.3.02 Costo de ventas [EGRESO DEUDORA grupo padre=5.3]",
                "      5.3.02.001 Costo de ventas [EGRESO DEUDORA hoja padre=5.3.02]");
        assertThat(contar("SELECT COUNT(*) FROM cuentas_contables WHERE codigo IN ('1.1.04','1.1.04.001','5.3','5.3.01','5.3.01.001',"
                + "'5.3.01.002','5.3.01.003','5.3.02','5.3.02.001') AND activa = b'1'")).isEqualTo(9);

        assertThat(texto("SELECT c.codigo FROM parametros_contables p JOIN cuentas_contables c ON c.id = p.cuenta_contable_id "
                + "WHERE p.clave = 'COMPRAS_MERCADERIA'")).isEqualTo("5.3.01.001");
        assertThat(texto("SELECT c.codigo FROM parametros_contables p JOIN cuentas_contables c ON c.id = p.cuenta_contable_id "
                + "WHERE p.clave = 'CPP_CUENTA_COMPRAS'")).as("la clave de la OC no cambia").isEqualTo("5.1.01");
    }

    @Test
    @DisplayName("es idempotente: la segunda corrida no crea ni cambia nada")
    void idempotente() throws Exception {
        V54__cuentas_inventario_costo_ventas.aplicar(con);
        String huella = texto("SELECT GROUP_CONCAT(CONCAT_WS('|', id, codigo, nombre, cuenta_padre_id) ORDER BY id) FROM cuentas_contables");
        long params = contar("SELECT COUNT(*) FROM parametros_contables");

        V54__cuentas_inventario_costo_ventas.aplicar(con);

        assertThat(texto("SELECT GROUP_CONCAT(CONCAT_WS('|', id, codigo, nombre, cuenta_padre_id) ORDER BY id) FROM cuentas_contables"))
                .isEqualTo(huella);
        assertThat(contar("SELECT COUNT(*) FROM parametros_contables")).isEqualTo(params);
    }

    @Test
    @DisplayName("completa un estado parcial: 5.3 ya creada igual a la de la migración se conserva y se crea el resto")
    void completaEstadoParcial() throws Exception {
        try (Statement st = con.createStatement()) {
            cuenta(st, 60, "5.3", "Costo de ventas", "EGRESO", "DEUDORA", 2, "26", false);
        }
        V54__cuentas_inventario_costo_ventas.aplicar(con);

        assertThat(contar("SELECT COUNT(*) FROM cuentas_contables WHERE codigo = '5.3'")).isEqualTo(1);
        assertThat(texto("SELECT id FROM cuentas_contables WHERE codigo = '5.3'")).isEqualTo("60");
        assertThat(texto("SELECT p.codigo FROM cuentas_contables c JOIN cuentas_contables p ON p.id = c.cuenta_padre_id "
                + "WHERE c.codigo = '5.3.01'")).isEqualTo("5.3");
        assertThat(contar("SELECT COUNT(*) FROM cuentas_contables WHERE codigo LIKE '5.3%'")).isEqualTo(7);
    }

    @Test
    @DisplayName("5.3 ocupada por otra cuenta: aborta con un mensaje que nombra el código y la cuenta existente, y no crea nada")
    void abortaSi53Ocupada() throws Exception {
        try (Statement st = con.createStatement()) {
            cuenta(st, 60, "5.3", "Gastos Extraordinarios", "EGRESO", "DEUDORA", 2, "26", false);
        }
        long cuentas = contar("SELECT COUNT(*) FROM cuentas_contables");

        assertThatThrownBy(() -> V54__cuentas_inventario_costo_ventas.aplicar(con))
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("5.3")
                .hasMessageContaining("Gastos Extraordinarios")
                .hasMessageContaining("Costo de ventas");

        assertThat(contar("SELECT COUNT(*) FROM cuentas_contables")).isEqualTo(cuentas);
        assertThat(contar("SELECT COUNT(*) FROM parametros_contables WHERE clave = 'COMPRAS_MERCADERIA'")).isZero();
    }

    @Test
    @DisplayName("1.1.04 ocupada: aborta aunque 5.3 esté libre, sin crear ninguna cuenta")
    void abortaSi1104Ocupada() throws Exception {
        try (Statement st = con.createStatement()) {
            cuenta(st, 61, "1.1.04", "Gastos Anticipados", "ACTIVO", "DEUDORA", 3, "33", true);
        }
        long cuentas = contar("SELECT COUNT(*) FROM cuentas_contables");

        assertThatThrownBy(() -> V54__cuentas_inventario_costo_ventas.aplicar(con))
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("1.1.04")
                .hasMessageContaining("Gastos Anticipados");

        assertThat(contar("SELECT COUNT(*) FROM cuentas_contables")).isEqualTo(cuentas);
    }

    @Test
    @DisplayName("una hoja ocupada con el mismo nombre pero otro tipo también aborta")
    void abortaSiDifiereElTipo() throws Exception {
        try (Statement st = con.createStatement()) {
            cuenta(st, 62, "5.3.02.001", "Costo de ventas", "INGRESO", "ACREEDORA", 4, "NULL", true);
        }
        assertThatThrownBy(() -> V54__cuentas_inventario_costo_ventas.aplicar(con))
                .isInstanceOf(FlywayException.class).hasMessageContaining("5.3.02.001");
        assertThat(contar("SELECT COUNT(*) FROM cuentas_contables WHERE codigo LIKE '5.3%'")).isEqualTo(1);
    }

    @Test
    @DisplayName("si falta la cuenta padre «5» o «1.1» aborta con mensaje claro")
    void abortaSiFaltaElPadre() throws Exception {
        try (Statement st = con.createStatement()) {
            st.execute("DELETE FROM parametros_contables");
            st.execute("DELETE FROM cuentas_contables WHERE codigo = '5.1.01'");
            st.execute("DELETE FROM cuentas_contables WHERE codigo = '5.1'");
            st.execute("DELETE FROM cuentas_contables WHERE codigo = '5'");
        }
        assertThatThrownBy(() -> V54__cuentas_inventario_costo_ventas.aplicar(con))
                .isInstanceOf(FlywayException.class).hasMessageContaining("«5»");
    }

    @Test
    @DisplayName("si COMPRAS_MERCADERIA ya apuntaba a otra cuenta, queda en 5.3.01.001")
    void actualizaElParametro() throws Exception {
        try (Statement st = con.createStatement()) {
            st.execute("INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id) VALUES "
                    + "('COMPRAS_MERCADERIA', 'otra', 5)");
        }
        V54__cuentas_inventario_costo_ventas.aplicar(con);
        assertThat(texto("SELECT c.codigo FROM parametros_contables p JOIN cuentas_contables c ON c.id = p.cuenta_contable_id "
                + "WHERE p.clave = 'COMPRAS_MERCADERIA'")).isEqualTo("5.3.01.001");
        assertThat(contar("SELECT COUNT(*) FROM parametros_contables WHERE clave = 'COMPRAS_MERCADERIA'")).isEqualTo(1);
    }
}
