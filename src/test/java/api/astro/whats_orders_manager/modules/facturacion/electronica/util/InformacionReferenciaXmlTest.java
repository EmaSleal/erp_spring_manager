package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T-032 (criterio 11): bloque InformacionReferencia reutilizable. */
@DisplayName("T-032 — InformacionReferenciaXml")
class InformacionReferenciaXmlTest {

    @Test
    @DisplayName("arma las etiquetas del XSD con los valores recibidos y la fecha en hora de Costa Rica")
    void construye() {
        String xml = InformacionReferenciaXml.construir("10", "50601", LocalDateTime.of(2026, 1, 5, 8, 0, 0), "16",
                "Sustituye comprobante rechazado");

        assertThat(xml).isEqualTo("<InformacionReferencia><TipoDocIR>10</TipoDocIR><Numero>50601</Numero>"
                + "<FechaEmisionIR>2026-01-05T08:00:00-06:00</FechaEmisionIR><Codigo>16</Codigo>"
                + "<Razon>Sustituye comprobante rechazado</Razon></InformacionReferencia>");
    }

    @Test
    @DisplayName("escapa & y < en la razón")
    void escapaRazon() {
        String xml = InformacionReferenciaXml.construir("01", "1", LocalDateTime.of(2026, 1, 5, 8, 0), "01", "A & B < C");

        assertThat(xml).contains("<Razon>A &amp; B &lt; C</Razon>");
        assertThat(XmlValidator.validarXml(xml)).isTrue();
    }

    @Test
    @DisplayName("sin fecha del documento referenciado → error")
    void sinFecha() {
        assertThatThrownBy(() -> InformacionReferenciaXml.construir("01", "1", null, "01", "abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
