package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T-032 (criterios 5 y 25): validación del pedido de nota de crédito (400). */
@DisplayName("T-032 — validación del pedido de nota de crédito")
class NotaFacturaPedidoTest {

    private static final String RAZON = "Anulación total";

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"01", "13"})
    @DisplayName("01 y 13 con razón válida se aceptan")
    void codigosHabilitados(String codigo) {
        assertThat(NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, codigo, RAZON).getCodigo())
                .isEqualTo(codigo);
        assertThat(NotaFacturaService.validarPedido(null, codigo, RAZON)).isNotNull();
    }

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"02", "04", "05", "06", "08", "09", "10", "11", "12", "14", "99"})
    @DisplayName("otro código válido de la nota 9 → «disponible en una fase posterior»")
    void codigosDeFasePosterior(String codigo) {
        assertThatThrownBy(() -> NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, codigo, RAZON))
                .isInstanceOf(NotaFacturaPedidoInvalidoException.class)
                .hasMessageContaining("disponible en una fase posterior");
    }

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"07", "15", "16", "17"})
    @DisplayName("códigos de comprobante sustituto o de recibo de pago → no se usan en nota de crédito")
    void codigosNoValidosEnNota(String codigo) {
        assertThatThrownBy(() -> NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, codigo, RAZON))
                .isInstanceOf(NotaFacturaPedidoInvalidoException.class)
                .hasMessageContaining("no se usa en una nota de crédito");
    }

    @ParameterizedTest(name = "código «{0}»")
    @ValueSource(strings = {"03", "00", "1", "", "XX"})
    @DisplayName("código inexistente → inválido")
    void codigoInexistente(String codigo) {
        assertThatThrownBy(() -> NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, codigo, RAZON))
                .isInstanceOf(NotaFacturaPedidoInvalidoException.class)
                .hasMessageContaining("Código de referencia inválido");
    }

    @Test
    @DisplayName("tipo DEBITO → 400")
    void debito() {
        assertThatThrownBy(() -> NotaFacturaService.validarPedido(TipoNotaFactura.DEBITO, "01", RAZON))
                .isInstanceOf(NotaFacturaPedidoInvalidoException.class);
    }

    @Test
    @DisplayName("razón de 2 o 181 caracteres → 400; de 3 y 180 → válida")
    void longitudDeLaRazon() {
        assertThatThrownBy(() -> NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, "01", "ab"))
                .isInstanceOf(NotaFacturaPedidoInvalidoException.class).hasMessageContaining("entre 3 y 180");
        assertThatThrownBy(() -> NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, "01", "x".repeat(181)))
                .isInstanceOf(NotaFacturaPedidoInvalidoException.class).hasMessageContaining("entre 3 y 180");
        assertThatThrownBy(() -> NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, "01", null))
                .isInstanceOf(NotaFacturaPedidoInvalidoException.class);
        assertThat(NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, "01", "abc"))
                .isEqualTo(CodigoReferenciaNota.ANULA_DOCUMENTO);
        assertThat(NotaFacturaService.validarPedido(TipoNotaFactura.CREDITO, "01", "x".repeat(180)))
                .isEqualTo(CodigoReferenciaNota.ANULA_DOCUMENTO);
    }
}
