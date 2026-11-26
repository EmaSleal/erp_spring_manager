package api.astro.whats_orders_manager.modules.facturacion.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-057 (criterios 5 y 15): el elemento «Enviar a Hacienda» de la fila de la lista solo se muestra sin comprobante, sin
 * clave externa y si la factura no es SIMPLIFICADO. Es la única entrada de emisión de facturas.html; el servidor igual
 * responde 409 (PoliticaEmision).
 */
class FacturasListaBotonEmisionTest {

    @Test
    @DisplayName("el botón de la lista exige sin comprobante, régimen distinto de SIMPLIFICADO y sin clave externa")
    void condicionDelBoton() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/modules/facturacion/facturas.html"));
        int i = html.indexOf("onclick=\"enviarAHacienda(this)\"");
        assertThat(i).isPositive();
        String antes = html.substring(html.lastIndexOf("<li ", i), i);
        assertThat(antes).contains("comprobanteElectronico == null").contains("regimenEmisor?.name() != 'SIMPLIFICADO'")
                .contains("comprobanteExternoClave == null");
        assertThat(html.split("onclick=\"enviarAHacienda\\(this\\)\"", -1)).hasSize(2);
    }
}
