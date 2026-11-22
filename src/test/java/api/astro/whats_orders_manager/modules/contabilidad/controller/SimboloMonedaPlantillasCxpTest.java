package api.astro.whats_orders_manager.modules.contabilidad.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-052 (P-023 §4): el detalle de asientos y la pantalla de cuentas por pagar toman el símbolo de MonedaService
 * (atributo simboloMoneda del modelo) y no un símbolo fijo. Se leen las plantillas como texto.
 */
@DisplayName("T-052 — plantillas del módulo con el símbolo de MonedaService")
class SimboloMonedaPlantillasCxpTest {

    private static final Path PLANTILLAS = Path.of("src/main/resources/templates/modules/contabilidad");

    private static String leer(String relativo) throws IOException {
        return Files.readString(PLANTILLAS.resolve(relativo), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("asientos/detalle.html formatea cada importe con simboloMoneda, sin «$» fijo")
    void detalleDeAsientos() throws IOException {
        String html = leer("asientos/detalle.html");
        assertThat(html).contains("simboloMoneda");
        assertThat(html).doesNotContain("'$' +").doesNotContain("\"$\" +").doesNotContain(">$0.00<");
    }

    @Test
    @DisplayName("cuentas-pagar.html no trae el símbolo de colón fijo; usa simboloMoneda")
    void cuentasPagar() throws IOException {
        String html = leer("cuentas-pagar.html");
        assertThat(html).contains("simboloMoneda");
        assertThat(html).doesNotContain("&#x20A1;");
    }
}
