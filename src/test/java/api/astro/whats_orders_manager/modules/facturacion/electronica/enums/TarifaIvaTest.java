package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** T-052: catálogo de tarifas de IVA (Hacienda v4.4) compartido por el XML de venta y el desglose de la CxP. */
@DisplayName("T-052 — TarifaIva: catálogo de códigos de tarifa")
class TarifaIvaTest {

    /** Misma tabla que tenía XmlGeneratorServiceImpl.resolverCodigoTarifaIVA antes de extraer el catálogo. */
    @Test
    @DisplayName("el código por porcentaje es idéntico al que generaba el XML de venta (0→10, 0,5→09, 1→02, 2→03, 4→04, 8→07, resto→08)")
    void codigoPorPorcentajeConservaElXml() {
        assertThat(TarifaIva.codigoPorPorcentaje(null)).isEqualTo("10");
        assertThat(TarifaIva.codigoPorPorcentaje(BigDecimal.ZERO)).isEqualTo("10");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("0.00"))).isEqualTo("10");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("0.5"))).isEqualTo("09");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("1.00"))).isEqualTo("02");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("2"))).isEqualTo("03");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("4"))).isEqualTo("04");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("8"))).isEqualTo("07");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("13"))).isEqualTo("08");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("13.00"))).isEqualTo("08");
        assertThat(TarifaIva.codigoPorPorcentaje(new BigDecimal("5"))).isEqualTo("08");
    }

    @Test
    @DisplayName("deCodigo resuelve los once códigos 01..11 con su porcentaje y rechaza los desconocidos")
    void deCodigo() {
        assertThat(TarifaIva.deCodigo("08")).map(TarifaIva::getPorcentaje).hasValueSatisfying(
                p -> assertThat(p).isEqualByComparingTo("13"));
        assertThat(TarifaIva.deCodigo("07").orElseThrow().getPorcentaje()).isEqualByComparingTo("8");
        assertThat(TarifaIva.deCodigo("09").orElseThrow().getPorcentaje()).isEqualByComparingTo("0.5");
        assertThat(TarifaIva.deCodigo("10").orElseThrow().getPorcentaje()).isEqualByComparingTo("0");
        for (int i = 1; i <= 11; i++) {
            assertThat(TarifaIva.deCodigo(String.format("%02d", i))).isPresent();
        }
        assertThat(TarifaIva.deCodigo("12")).isEqualTo(Optional.empty());
        assertThat(TarifaIva.deCodigo("8")).isEmpty();
        assertThat(TarifaIva.deCodigo(null)).isEmpty();
    }
}
