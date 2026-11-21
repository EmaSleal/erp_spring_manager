package api.astro.whats_orders_manager.modules.facturacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T-049 — tramos de antigüedad: 0–7, 8–30, 31–90, 91–180, >180")
class TramoAntiguedadTest {

    @Test
    @DisplayName("límites de cada tramo")
    void limites() {
        assertThat(TramoAntiguedad.de(0)).isEqualTo(TramoAntiguedad.DE_0_A_7);
        assertThat(TramoAntiguedad.de(7)).isEqualTo(TramoAntiguedad.DE_0_A_7);
        assertThat(TramoAntiguedad.de(8)).isEqualTo(TramoAntiguedad.DE_8_A_30);
        assertThat(TramoAntiguedad.de(30)).isEqualTo(TramoAntiguedad.DE_8_A_30);
        assertThat(TramoAntiguedad.de(31)).isEqualTo(TramoAntiguedad.DE_31_A_90);
        assertThat(TramoAntiguedad.de(90)).isEqualTo(TramoAntiguedad.DE_31_A_90);
        assertThat(TramoAntiguedad.de(91)).isEqualTo(TramoAntiguedad.DE_91_A_180);
        assertThat(TramoAntiguedad.de(180)).isEqualTo(TramoAntiguedad.DE_91_A_180);
        assertThat(TramoAntiguedad.de(181)).isEqualTo(TramoAntiguedad.MAS_DE_180);
        assertThat(TramoAntiguedad.de(5000)).isEqualTo(TramoAntiguedad.MAS_DE_180);
    }
}
