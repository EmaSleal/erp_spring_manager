package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** T-034 (D2, D3, D4): los códigos de unidad del seed (y de un dump opcional) pertenecen al enum del XSD. */
@DisplayName("T-034 — Unidades de medida FE contra el XSD")
class UnidadMedidaFeTest {

    private static final Path SEED = Path.of("database/seeds/10_presentacion.sql");

    @Test
    @DisplayName("el enum se lee del XSD (101 valores) y conoce los códigos de uso común")
    void enumDelXsd() {
        assertThat(UnidadMedidaFe.valores()).hasSize(101).contains("Unid", "L", "Kg", "m²", "m³", "m/s²", "kg/m³", "Otros");
        assertThat(UnidadMedidaFe.esValida("L")).isTrue();
        assertThat(UnidadMedidaFe.esValida("l")).isFalse();
        assertThat(UnidadMedidaFe.esValida("Galon")).isFalse();
        assertThat(UnidadMedidaFe.esValida(null)).isFalse();
    }

    @Test
    @DisplayName("seed 10_presentacion.sql: sin BOM, sin mojibake y con todos los codigo_unidad_fe en el enum")
    void seedValido() throws Exception {
        byte[] bytes = Files.readAllBytes(SEED);
        assertThat(bytes.length > 2 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF)
                .as("BOM").isFalse();
        String texto = new String(bytes, StandardCharsets.UTF_8);
        assertThat(texto).doesNotContain("┬", "├", "�");

        List<String> codigos = new ArrayList<>();
        for (String linea : texto.split("\n")) {
            if (!linea.startsWith("INSERT")) continue;
            // ... VALUES (id,'nombre',NULL,NULL,NULL,NULL,'codigo',empacado);
            String valores = linea.substring(linea.indexOf("VALUES"));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "NULL,NULL,NULL,NULL,(NULL|'([^']*)'),([01])\\);").matcher(valores);
            assertThat(m.find()).as(linea).isTrue();
            if (m.group(2) != null) codigos.add(m.group(2));
        }
        assertThat(codigos).hasSize(37);
        assertThat(codigos).allSatisfy(c -> assertThat(UnidadMedidaFe.esValida(c)).as(c).isTrue());
    }

    @Test
    @DisplayName("archivo externo (-DunidadesFe.file=...): cada línea no vacía está en el enum; se omite si no se indica")
    void archivoExterno() throws Exception {
        String ruta = System.getProperty("unidadesFe.file");
        Assumptions.assumeTrue(ruta != null && !ruta.isBlank(), "unidadesFe.file no indicado");
        List<String> lineas = Files.readAllLines(Path.of(ruta), StandardCharsets.UTF_8);
        for (String linea : lineas) {
            if (linea.isEmpty()) continue;
            assertThat(UnidadMedidaFe.esValida(linea)).as("código '" + linea + "'").isTrue();
        }
    }
}
