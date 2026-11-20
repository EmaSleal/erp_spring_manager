package api.astro.whats_orders_manager.modules.facturacion.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo en CRC de la aplicación de un saldo a favor a una factura (T-048, ADR-055, ADR-057).
 *
 * <p>Asiento: débito a «Saldos a favor de clientes» por {@code monto × TC origen} (con el que se acreditó), crédito a
 * cuentas por cobrar por {@code monto × TC de la factura destino} (con el que se cargó) y la diferencia a
 * DIFERENCIAL_CAMBIARIO_INGRESO (haber) o _GASTO (debe), el diferencial realizado.
 *
 * <p>Tramos contra el TC del día (solo USD): {@code diferencialSaldo = monto × (TC día − TC origen)} y
 * {@code diferencialCxc = monto × (TC día − TC factura)}; su neto es {@code diferencialCxc − diferencialSaldo} y es
 * exactamente la diferencia del asiento ({@code debeSaldo − haberCxc}): positivo = ganancia, negativo = pérdida. Cada
 * tramo se calcula como la diferencia de importes ya redondeados a 2 decimales, así el neto cuadra al céntimo.
 * En CRC no hay tipo de cambio ni diferencial.
 */
public record CalculoAplicacionSaldoFavor(BigDecimal debeSaldoCrc, BigDecimal haberCxcCrc, BigDecimal diferencialSaldo,
                                          BigDecimal diferencialCxc, BigDecimal diferencialNeto) {

    /**
     * @param monto             monto aplicado, en la moneda original
     * @param tipoCambioOrigen  TC con el que se creó el saldo (1 en CRC)
     * @param tipoCambioFactura TC de la factura destino (1 en CRC)
     * @param tipoCambioDia     TC de venta del día (null en CRC: sin diferencial)
     */
    public static CalculoAplicacionSaldoFavor calcular(BigDecimal monto, BigDecimal tipoCambioOrigen,
                                                       BigDecimal tipoCambioFactura, BigDecimal tipoCambioDia) {
        BigDecimal debe = aCrc(monto, tipoCambioOrigen);
        BigDecimal haber = aCrc(monto, tipoCambioFactura);
        if (tipoCambioDia == null) {
            return new CalculoAplicacionSaldoFavor(debe, haber, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                debe.subtract(haber));
        }
        BigDecimal valorDelDia = aCrc(monto, tipoCambioDia);
        BigDecimal tramoSaldo = valorDelDia.subtract(debe);
        BigDecimal tramoCxc = valorDelDia.subtract(haber);
        return new CalculoAplicacionSaldoFavor(debe, haber, tramoSaldo, tramoCxc, tramoCxc.subtract(tramoSaldo));
    }

    private static BigDecimal aCrc(BigDecimal monto, BigDecimal tipoCambio) {
        return monto.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
    }
}
