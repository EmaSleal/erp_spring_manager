package api.astro.whats_orders_manager.modules.facturacion.correo;

import api.astro.whats_orders_manager.modules.facturacion.correo.service.ContenidoCorreoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.ContenidoCorreoComprobante.DatosCorreoComprobante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** T-055: asunto, cuerpo HTML (con el nombre del receptor escapado) y nombres de los adjuntos. */
@DisplayName("T-055 — contenido del correo del comprobante")
class ContenidoCorreoComprobanteTest {

    private static final String CLAVE = "50601012600310123456700100001010000000042100000042";

    private static DatosCorreoComprobante datos(boolean nota, String receptor) {
        return new DatosCorreoComprobante(nota, receptor, "00100001010000000042", CLAVE,
                LocalDateTime.of(2026, 11, 3, 14, 30), new BigDecimal("1630.5"), "CRC", "Monrachem S.A.");
    }

    @Test
    @DisplayName("asunto: tipo de documento, consecutivo y nombre del emisor; con prefijo [SANDBOX] en SANDBOX")
    void asunto() {
        assertThat(ContenidoCorreoComprobante.asunto(datos(false, "Ana"), false))
                .isEqualTo("Factura electrónica 00100001010000000042 - Monrachem S.A.");
        assertThat(ContenidoCorreoComprobante.asunto(datos(true, "Ana"), false))
                .isEqualTo("Nota de crédito electrónica 00100001010000000042 - Monrachem S.A.");
        assertThat(ContenidoCorreoComprobante.asunto(datos(false, "Ana"), true))
                .isEqualTo("[SANDBOX] Factura electrónica 00100001010000000042 - Monrachem S.A.");
    }

    @Test
    @DisplayName("el nombre del receptor se escapa: no puede inyectar HTML")
    void escapaElNombreDelReceptor() {
        String html = ContenidoCorreoComprobante.html(datos(false, "<script>alert('x')</script> & \"Cía\""), false);

        assertThat(html).doesNotContain("<script>").doesNotContain("alert('x')");
        assertThat(html).contains("&lt;script&gt;").contains("&amp;").contains("&quot;");
    }

    @Test
    @DisplayName("cuerpo: tipo, consecutivo, clave, fecha dd/MM/yyyy y total con moneda")
    void cuerpo() {
        String html = ContenidoCorreoComprobante.html(datos(false, "Ana Pérez"), true);

        assertThat(html).contains("Ana Pérez").contains("Factura electrónica").contains("00100001010000000042")
                .contains(CLAVE).contains("03/11/2026").contains("CRC 1630.50");
        assertThat(ContenidoCorreoComprobante.html(datos(true, "Ana"), true)).contains("Nota de crédito electrónica");
    }

    @Test
    @DisplayName("sin Reply-To el cuerpo dice que el correo no recibe respuestas; con Reply-To no")
    void noRecibeRespuestas() {
        assertThat(ContenidoCorreoComprobante.html(datos(false, "Ana"), false)).contains("Este correo no recibe respuestas");
        assertThat(ContenidoCorreoComprobante.html(datos(false, "Ana"), true)).doesNotContain("no recibe respuestas");
    }

    @Test
    @DisplayName("los adjuntos se nombran con la clave")
    void nombresDeAdjuntos() {
        assertThat(ContenidoCorreoComprobante.nombrePdf(CLAVE)).isEqualTo(CLAVE + ".pdf");
        assertThat(ContenidoCorreoComprobante.nombreXml(CLAVE)).isEqualTo(CLAVE + ".xml");
        assertThat(ContenidoCorreoComprobante.nombreRespuestaHacienda(CLAVE)).isEqualTo(CLAVE + "-respuesta-hacienda.xml");
    }
}
