package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-056 (criterio 3): «hay facturas, notas ni CxP con fecha igual o posterior al inicio». Factura: la fecha de creación
 * (la de emisión) o la fecha contable; nota: fecha de emisión o fecha contable; CxP: fecha del comprobante, o la de
 * registro si no tiene. Se cuentan también las anuladas. Filas mínimas por SQL (H2, sin claves foráneas).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(ConsultaDocumentosPorFecha.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-056 — documentos con fecha igual o posterior a un inicio de tramo (H2)")
class ConsultaDocumentosPorFechaJpaTest {

    private static final LocalDate INICIO = LocalDate.of(2027, 1, 1);

    @Autowired private ConsultaDocumentosPorFecha consulta;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        limpiar();
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("DELETE FROM nota_factura");
        jdbc.execute("DELETE FROM factura");
        jdbc.execute("DELETE FROM cuentas_por_pagar");
    }

    private void factura(String creada, String contable) {
        jdbc.update("INSERT INTO factura (id_cliente, anulada, monto_acreditado, acreditada_totalmente, create_date, fecha_contable) "
                + "VALUES (1, FALSE, 0, FALSE, " + creada + ", " + contable + ")");
    }

    private void nota(String emision, String contable) {
        jdbc.update("INSERT INTO nota_factura (factura_id, tipo, codigo_referencia, razon, estado, modalidad, fecha_emision, fecha_contable, "
                + "periodo_fiscal_atribucion, comprobante_referencia_id, clave_referencia, fecha_emision_referencia, moneda, tipo_cambio, "
                + "subtotal, descuento, impuesto, total, usuario_creador, create_date) VALUES (1, 'CREDITO', '01', 'r', 'APLICADA', "
                + "'TOTAL', " + emision + ", " + contable + ", '2026-11', 1, 'k', TIMESTAMP '2026-01-01 00:00:00', 'CRC', 1, 0, 0, 0, 0, 'u', "
                + "TIMESTAMP '2026-01-01 00:00:00')");
    }

    private void cxp(String emision, String comprobante) {
        jdbc.update("INSERT INTO cuentas_por_pagar (numero, proveedor_id, descripcion, monto, saldo_pendiente, fecha_emision, "
                + "fecha_comprobante, estado_aceptacion, estado) VALUES ('CP" + System.nanoTime() % 100000 + "', 1, 'c', 1, 1, "
                + emision + ", " + comprobante + ", 'PENDIENTE', 'PENDIENTE')");
    }

    @Test
    @DisplayName("sin documentos: false")
    void sinDocumentos() {
        assertThat(consulta.existenDesde(INICIO)).isFalse();
    }

    @Test
    @DisplayName("facturas: por fecha de creación o fecha contable, el mismo día cuenta y el día anterior no")
    void facturas() {
        factura("TIMESTAMP '2026-12-31 23:59:59'", "DATE '2026-12-31'");
        assertThat(consulta.existenDesde(INICIO)).isFalse();

        factura("TIMESTAMP '2027-01-01 00:00:00'", "NULL");
        assertThat(consulta.existenDesde(INICIO)).as("creada el mismo día a las 00:00").isTrue();
        limpiar();

        factura("TIMESTAMP '2026-12-31 23:59:59'", "DATE '2027-01-02'");
        assertThat(consulta.existenDesde(INICIO)).as("fecha contable posterior").isTrue();
    }

    @Test
    @DisplayName("notas: por fecha de emisión o fecha contable")
    void notas() {
        nota("TIMESTAMP '2026-12-31 23:59:59'", "DATE '2026-12-31'");
        assertThat(consulta.existenDesde(INICIO)).isFalse();
        nota("TIMESTAMP '2027-03-01 10:00:00'", "DATE '2027-03-01'");
        assertThat(consulta.existenDesde(INICIO)).isTrue();
        limpiar();
        nota("TIMESTAMP '2026-12-31 23:59:59'", "DATE '2027-01-01'");
        assertThat(consulta.existenDesde(INICIO)).as("fecha contable el mismo día").isTrue();
    }

    @Test
    @DisplayName("CxP: fecha del comprobante, o la de registro si no tiene")
    void cuentasPorPagar() {
        cxp("DATE '2027-06-01'", "DATE '2026-12-31'");
        assertThat(consulta.existenDesde(INICIO)).as("manda la fecha del comprobante, anterior al inicio").isFalse();

        cxp("DATE '2027-01-01'", "NULL");
        assertThat(consulta.existenDesde(INICIO)).as("sin comprobante: la de registro").isTrue();
        limpiar();

        cxp("DATE '2026-01-01'", "DATE '2027-01-01'");
        assertThat(consulta.existenDesde(INICIO)).as("comprobante el mismo día").isTrue();
    }
}
