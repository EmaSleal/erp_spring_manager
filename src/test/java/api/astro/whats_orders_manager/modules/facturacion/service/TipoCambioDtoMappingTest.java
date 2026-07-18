package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.TipoCambioDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the pure TipoCambio to TipoCambioDTO mapping logic.
 * Zero mocks needed -- pure function test.
 */
@DisplayName("TipoCambioDTO mapping -- pure logic tests")
class TipoCambioDtoMappingTest {

    // Same mapping logic as in TipoCambioRestController.toDTO
    private TipoCambioDTO toDTO(TipoCambio tc) {
        return TipoCambioDTO.builder()
                .id(tc.getId())
                .monedaOrigen(tc.getMonedaOrigen().getCodigo())
                .monedaDestino(tc.getMonedaDestino().getCodigo())
                .simboloMoneda(tc.getMonedaDestino().getSimbolo())
                .fecha(tc.getFecha())
                .tasaCompra(tc.getTasaCompra())
                .tasaVenta(tc.getTasaVenta())
                .fuente(tc.getFuente().name())
                .build();
    }

    @Test
    @DisplayName("maps USD TipoCambio to DTO with correct codigo and simbolo")
    void toDTO_usdRate_mapsCorrectly() {
        TipoCambio tc = TipoCambio.builder()
                .id(1L)
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.USD)
                .fecha(LocalDate.of(2026, 7, 14))
                .tasaCompra(new BigDecimal("530.00000"))
                .tasaVenta(new BigDecimal("532.15000"))
                .fuente(FuenteTipoCambio.MANUAL)
                .activo(true)
                .build();

        TipoCambioDTO dto = toDTO(tc);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getMonedaDestino()).isEqualTo("USD");
        assertThat(dto.getSimboloMoneda()).isEqualTo("$");
        assertThat(dto.getTasaVenta()).isEqualByComparingTo("532.15000");
        assertThat(dto.getFuente()).isEqualTo("MANUAL");
    }

    @Test
    @DisplayName("maps EUR TipoCambio to DTO with euro simbolo")
    void toDTO_eurRate_hasCurrencySimboloEuro() {
        TipoCambio tc = TipoCambio.builder()
                .id(2L)
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.EUR)
                .fecha(LocalDate.of(2026, 7, 14))
                .tasaCompra(new BigDecimal("575.00000"))
                .tasaVenta(new BigDecimal("580.50000"))
                .fuente(FuenteTipoCambio.BCCR)
                .activo(true)
                .build();

        TipoCambioDTO dto = toDTO(tc);

        assertThat(dto.getMonedaDestino()).isEqualTo("EUR");
        assertThat(dto.getSimboloMoneda()).isEqualTo("€");
        assertThat(dto.getFuente()).isEqualTo("BCCR");
    }
}
