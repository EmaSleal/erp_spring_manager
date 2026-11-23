package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-054 (ADR-067 §3, criterio 5): IVA acreditable y no acreditable por línea, redondeado a 2 decimales con HALF_UP.
 * PLENO = IVA; USO_COMUN = IVA × porcentaje del año; LIMITADO = base × tarifa límite (sin pasar del IVA);
 * NO_ACREDITABLE = 0. El no acreditable es IVA − acreditable.
 */
@DisplayName("T-054 — cálculo del IVA acreditable por tratamiento")
class IvaCompraCalculoTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("PLENO: todo el IVA es acreditable")
    void pleno() {
        var r = IvaCompraCalculo.calcular(d("1000.00"), d("130.00"), TratamientoIvaCompra.PLENO, null, null);
        assertThat(r.acreditable()).isEqualByComparingTo("130.00");
        assertThat(r.noAcreditable()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("USO_COMUN: IVA × porcentaje de prorrata del año (85,50 % de 130 = 111,15; no acreditable 18,85)")
    void usoComun() {
        var r = IvaCompraCalculo.calcular(d("1000.00"), d("130.00"), TratamientoIvaCompra.USO_COMUN, null, d("85.50"));
        assertThat(r.acreditable()).isEqualByComparingTo("111.15");
        assertThat(r.noAcreditable()).isEqualByComparingTo("18.85");
    }

    @Test
    @DisplayName("USO_COMUN redondea mitad hacia arriba: 0,05 × 50 % = 0,025 → 0,03")
    void usoComunHalfUp() {
        var r = IvaCompraCalculo.calcular(d("0.38"), d("0.05"), TratamientoIvaCompra.USO_COMUN, null, d("50.00"));
        assertThat(r.acreditable()).isEqualByComparingTo("0.03");
        assertThat(r.noAcreditable()).isEqualByComparingTo("0.02");
    }

    @Test
    @DisplayName("USO_COMUN con 0 % y con 100 %")
    void usoComunExtremos() {
        assertThat(IvaCompraCalculo.calcular(d("1000"), d("130"), TratamientoIvaCompra.USO_COMUN, null, d("0")).acreditable())
                .isEqualByComparingTo("0.00");
        assertThat(IvaCompraCalculo.calcular(d("1000"), d("130"), TratamientoIvaCompra.USO_COMUN, null, d("100")).noAcreditable())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("USO_COMUN sin porcentaje de prorrata es un error de programación (el servicio lo valida antes)")
    void usoComunSinPorcentaje() {
        assertThatThrownBy(() -> IvaCompraCalculo.calcular(d("1000"), d("130"), TratamientoIvaCompra.USO_COMUN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("LIMITADO: una compra al 13 % limitada al 1 % → acreditable 10,00 (base × 1 %) y no acreditable 120,00")
    void limitadoTrece_aUno() {
        var r = IvaCompraCalculo.calcular(d("1000.00"), d("130.00"), TratamientoIvaCompra.LIMITADO, d("1"), null);
        assertThat(r.acreditable()).isEqualByComparingTo("10.00");
        assertThat(r.noAcreditable()).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("LIMITADO: el acreditable nunca supera el IVA de la línea")
    void limitadoConTope() {
        var r = IvaCompraCalculo.calcular(d("1000.00"), d("10.00"), TratamientoIvaCompra.LIMITADO, d("5"), null);
        assertThat(r.acreditable()).isEqualByComparingTo("10.00");
        assertThat(r.noAcreditable()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("LIMITADO sin tarifa límite es un error de programación (el validador lo rechaza antes)")
    void limitadoSinTarifa() {
        assertThatThrownBy(() -> IvaCompraCalculo.calcular(d("1000"), d("130"), TratamientoIvaCompra.LIMITADO, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NO_ACREDITABLE: acreditable 0 y todo el IVA va al costo o gasto de la línea")
    void noAcreditable() {
        var r = IvaCompraCalculo.calcular(d("1000.00"), d("130.00"), TratamientoIvaCompra.NO_ACREDITABLE, null, d("90"));
        assertThat(r.acreditable()).isEqualByComparingTo("0.00");
        assertThat(r.noAcreditable()).isEqualByComparingTo("130.00");
    }

    @Test
    @DisplayName("el resultado siempre tiene 2 decimales")
    void escala() {
        var r = IvaCompraCalculo.calcular(d("1000"), d("130"), TratamientoIvaCompra.PLENO, null, null);
        assertThat(r.acreditable().scale()).isEqualTo(2);
        assertThat(r.noAcreditable().scale()).isEqualTo(2);
    }
}
