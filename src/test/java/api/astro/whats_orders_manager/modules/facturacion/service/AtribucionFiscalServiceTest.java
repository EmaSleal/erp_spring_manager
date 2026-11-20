package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T-046 (criterios 3, 4 y 8; ADR-050 §1-§3): fecha contable y período fiscal de atribución por código. */
@DisplayName("T-046 — regla de atribución fiscal")
class AtribucionFiscalServiceTest {

    private static final LocalDate EMISION = LocalDate.of(2026, 11, 19);
    private static final LocalDateTime ORIGINAL = LocalDateTime.of(2026, 9, 29, 23, 40);
    private static final YearMonth MES_EMISION = YearMonth.of(2026, 11);
    private static final YearMonth MES_ORIGINAL = YearMonth.of(2026, 9);

    private final AtribucionFiscalService defecto = new AtribucionFiscalService("ORIGINAL");

    @Test
    @DisplayName("la fecha contable es la fecha del documento")
    void fechaContable() {
        assertThat(defecto.fechaContable(EMISION)).isEqualTo(EMISION);
        assertThatThrownBy(() -> defecto.fechaContable(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"01", "02", "06", "12", "07"})
    @DisplayName("01, 02, 06, 12 y 07: mes de emisión del documento")
    void mesDeEmision(String codigo) {
        assertThat(defecto.periodo(CodigoReferenciaNota.deCodigo(codigo).orElseThrow(), EMISION, ORIGINAL))
                .isEqualTo(MES_EMISION);
        assertThat(defecto.periodo(CodigoReferenciaNota.deCodigo(codigo).orElseThrow(), EMISION, null))
                .isEqualTo(MES_EMISION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"13", "14"})
    @DisplayName("13 y 14: mes de la factura original, aunque el parámetro del 15 sea EMISION")
    void mesDeLaOriginal(String codigo) {
        CodigoReferenciaNota c = CodigoReferenciaNota.deCodigo(codigo).orElseThrow();
        assertThat(defecto.periodo(c, EMISION, ORIGINAL)).isEqualTo(MES_ORIGINAL);
        assertThat(new AtribucionFiscalService("EMISION").periodo(c, EMISION, ORIGINAL)).isEqualTo(MES_ORIGINAL);
    }

    @Test
    @DisplayName("15 con ORIGINAL (valor por defecto): mes de la factura original")
    void codigo15_original() {
        assertThat(defecto.periodo(CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_ERROR_MATERIAL, EMISION, ORIGINAL))
                .isEqualTo(MES_ORIGINAL);
        assertThat(defecto.periodoTexto(CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_ERROR_MATERIAL, EMISION, ORIGINAL))
                .isEqualTo("2026-09");
        assertThat(new AtribucionFiscalService(" original ").periodo(
                CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_ERROR_MATERIAL, EMISION, ORIGINAL)).isEqualTo(MES_ORIGINAL);
    }

    @Test
    @DisplayName("15 con EMISION: mes de emisión de la sustituta")
    void codigo15_emision() {
        AtribucionFiscalService servicio = new AtribucionFiscalService("EMISION");
        assertThat(servicio.periodo(CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_ERROR_MATERIAL, EMISION, ORIGINAL))
                .isEqualTo(MES_EMISION);
        // el 07 no depende del parámetro del 15
        assertThat(servicio.periodo(CodigoReferenciaNota.SUSTITUYE_COMPROBANTE, EMISION, ORIGINAL))
                .isEqualTo(MES_EMISION);
    }

    @Test
    @DisplayName("un valor desconocido del parámetro del 15 falla al arrancar con un mensaje claro")
    void valorInvalido() {
        assertThatThrownBy(() -> new AtribucionFiscalService("MES_ANTERIOR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("facturacion.notas.atribucion.codigo-15").hasMessageContaining("MES_ANTERIOR");
        assertThatThrownBy(() -> new AtribucionFiscalService(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("un código sin regla o una atribución al original sin fecha del original fallan")
    void sinRegla() {
        assertThatThrownBy(() -> defecto.periodo(CodigoReferenciaNota.OTROS, EMISION, ORIGINAL))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> defecto.periodo(CodigoReferenciaNota.ANULA_DOCUMENTO_ERROR_MATERIAL, EMISION, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("una sustituta sin fecha contable o sin período no pasa la validación; una factura normal sí")
    void exigirAtribucion() {
        Factura sustituta = new Factura();
        sustituta.setNumeroFactura("FA-2");
        sustituta.setFacturaSustituida(new Factura());
        assertThatThrownBy(() -> AtribucionFiscalService.exigirAtribucion(sustituta))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("FA-2");
        sustituta.setFechaContable(EMISION);
        assertThatThrownBy(() -> AtribucionFiscalService.exigirAtribucion(sustituta))
                .isInstanceOf(IllegalStateException.class);
        sustituta.setPeriodoFiscalAtribucion("2026-09");
        AtribucionFiscalService.exigirAtribucion(sustituta);
        AtribucionFiscalService.exigirAtribucion(new Factura());
    }
}
