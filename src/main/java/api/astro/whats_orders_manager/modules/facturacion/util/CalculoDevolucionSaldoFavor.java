package api.astro.whats_orders_manager.modules.facturacion.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo en CRC del pago de una devolución en dinero de un saldo a favor (T-049, ADR-058 §3, ADR-055).
 *
 * <p>Asiento: débito a «Saldos a favor de clientes» por {@code monto × TC de origen} y crédito a la cuenta bancaria por
 * {@code monto × TC del día}; la diferencia va a DIFERENCIAL_CAMBIARIO_INGRESO (haber) o _GASTO (debe), realizado.
 *
 * <p>Un solo tramo, el del saldo contra el TC del día: {@code diferencialSaldo = monto × (TC día − TC origen)}, calculado
 * como diferencia de importes ya redondeados a 2 decimales. {@code diferencialNeto = debeSaldoCrc − haberBancoCrc}
 * (exactamente la diferencia del asiento): positivo = ganancia, negativo = pérdida; es {@code −diferencialSaldo}. En CRC
 * no hay tipo de cambio ni diferencial.
 */
public record CalculoDevolucionSaldoFavor(BigDecimal debeSaldoCrc, BigDecimal haberBancoCrc, BigDecimal diferencialSaldo,
                                          BigDecimal diferencialNeto) {

    /**
     * @param monto            monto devuelto, en la moneda original
     * @param tipoCambioOrigen TC con el que se creó el saldo (1 en CRC)
     * @param tipoCambioDia    TC de venta del día del pago (null en CRC: sin diferencial)
     */
    public static CalculoDevolucionSaldoFavor calcular(BigDecimal monto, BigDecimal tipoCambioOrigen,
                                                       BigDecimal tipoCambioDia) {
        BigDecimal debe = aCrc(monto, tipoCambioOrigen);
        if (tipoCambioDia == null) {
            return new CalculoDevolucionSaldoFavor(debe, debe, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));
        }
        BigDecimal haber = aCrc(monto, tipoCambioDia);
        return new CalculoDevolucionSaldoFavor(debe, haber, haber.subtract(debe), debe.subtract(haber));
    }

    private static BigDecimal aCrc(BigDecimal monto, BigDecimal tipoCambio) {
        return monto.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
    }
}
