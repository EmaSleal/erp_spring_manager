package api.astro.whats_orders_manager.modules.facturacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T-049 — cálculo del pago de una devolución de saldo a favor")
class CalculoDevolucionSaldoFavorTest {

    @Test
    @DisplayName("CRC: sin TC del día, sin diferencial; débito = crédito = monto")
    void crc() {
        CalculoDevolucionSaldoFavor c = CalculoDevolucionSaldoFavor.calcular(new BigDecimal("500.00"), BigDecimal.ONE, null);
        assertThat(c.debeSaldoCrc()).isEqualByComparingTo("500.00");
        assertThat(c.haberBancoCrc()).isEqualByComparingTo("500.00");
        assertThat(c.diferencialSaldo()).isEqualByComparingTo("0");
        assertThat(c.diferencialNeto()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("USD con el TC del día más alto que el de origen: pérdida (neto negativo)")
    void usdPerdida() {
        CalculoDevolucionSaldoFavor c = CalculoDevolucionSaldoFavor.calcular(new BigDecimal("100.00"),
                new BigDecimal("500.00000"), new BigDecimal("520.00000"));
        assertThat(c.debeSaldoCrc()).isEqualByComparingTo("50000.00");
        assertThat(c.haberBancoCrc()).isEqualByComparingTo("52000.00");
        assertThat(c.diferencialSaldo()).as("monto × (TC día − TC origen)").isEqualByComparingTo("2000.00");
        assertThat(c.diferencialNeto()).as("debe − haber del asiento").isEqualByComparingTo("-2000.00");
    }

    @Test
    @DisplayName("USD con el TC del día más bajo: ganancia (neto positivo)")
    void usdGanancia() {
        CalculoDevolucionSaldoFavor c = CalculoDevolucionSaldoFavor.calcular(new BigDecimal("100.00"),
                new BigDecimal("500.00000"), new BigDecimal("480.00000"));
        assertThat(c.diferencialSaldo()).isEqualByComparingTo("-2000.00");
        assertThat(c.diferencialNeto()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("el neto es exactamente debe − haber con importes redondeados a 2 decimales")
    void redondeo() {
        CalculoDevolucionSaldoFavor c = CalculoDevolucionSaldoFavor.calcular(new BigDecimal("33.33"),
                new BigDecimal("512.34567"), new BigDecimal("509.87654"));
        assertThat(c.diferencialNeto()).isEqualByComparingTo(c.debeSaldoCrc().subtract(c.haberBancoCrc()));
        assertThat(c.diferencialSaldo()).isEqualByComparingTo(c.haberBancoCrc().subtract(c.debeSaldoCrc()));
        assertThat(c.debeSaldoCrc().scale()).isEqualTo(2);
    }
}
