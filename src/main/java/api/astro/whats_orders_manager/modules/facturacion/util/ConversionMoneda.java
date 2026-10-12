package api.astro.whats_orders_manager.modules.facturacion.util;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas de conversión entre la moneda de un pago (P) y la del documento que paga (D),
 * según ADR-011. Funciones puras, sin acceso a datos.
 *
 * El tipo de cambio de la operación ({@code tcPago}) es siempre la tasa de venta, en CRC por
 * unidad, de la moneda extranjera involucrada en la fecha del pago:
 * <ul>
 *   <li>P = D = CRC → no hay moneda extranjera; tcPago = 1.</li>
 *   <li>P = D extranjera, o P = CRC y D extranjera → tasa de D (nunca 1, aunque el pago sea en CRC).</li>
 *   <li>P extranjera y D = CRC → tasa de P.</li>
 *   <li>P y D extranjeras distintas → no admitido.</li>
 * </ul>
 */
public final class ConversionMoneda {

    private ConversionMoneda() {
    }

    /** Moneda CRC cuando el valor es nulo. */
    public static MonedaFE oCrc(MonedaFE moneda) {
        return moneda != null ? moneda : MonedaFE.CRC;
    }

    /**
     * Determina la moneda extranjera cuyo tipo de cambio rige la operación.
     *
     * @return la moneda extranjera involucrada, o CRC si ambas son CRC
     * @throws IllegalArgumentException si pago y documento están en monedas extranjeras distintas
     */
    public static MonedaFE monedaExtranjera(MonedaFE monedaPago, MonedaFE monedaDocumento) {
        MonedaFE p = oCrc(monedaPago);
        MonedaFE d = oCrc(monedaDocumento);
        if (p != MonedaFE.CRC && d != MonedaFE.CRC && p != d) {
            throw new IllegalArgumentException(String.format(
                "No se admite un pago en %s sobre un documento en %s: pague en %s o en CRC",
                p.getCodigo(), d.getCodigo(), d.getCodigo()));
        }
        return p != MonedaFE.CRC ? p : d;
    }

    /**
     * Monto del pago expresado en la moneda del documento (HALF_UP, 2 decimales).
     * <ul>
     *   <li>P = D → monto.</li>
     *   <li>P = CRC, D extranjera → monto / tcPago.</li>
     *   <li>P extranjera, D = CRC → monto × tcPago.</li>
     * </ul>
     *
     * @throws IllegalArgumentException si las monedas extranjeras son distintas o falta el tipo de cambio
     */
    public static BigDecimal montoAplicado(BigDecimal monto, MonedaFE monedaPago, MonedaFE monedaDocumento,
                                           BigDecimal tcPago) {
        MonedaFE p = oCrc(monedaPago);
        MonedaFE d = oCrc(monedaDocumento);
        monedaExtranjera(p, d);
        if (p == d) {
            return monto.setScale(2, RoundingMode.HALF_UP);
        }
        validarTipoCambio(tcPago);
        return p == MonedaFE.CRC
            ? monto.divide(tcPago, 2, RoundingMode.HALF_UP)
            : monto.multiply(tcPago).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Monto del pago en CRC: monto si P = CRC; monto × tcPago en otro caso (HALF_UP, 2 decimales).
     */
    public static BigDecimal montoEnCrc(BigDecimal monto, MonedaFE monedaPago, BigDecimal tcPago) {
        if (oCrc(monedaPago) == MonedaFE.CRC) {
            return monto.setScale(2, RoundingMode.HALF_UP);
        }
        validarTipoCambio(tcPago);
        return monto.multiply(tcPago).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Monto de un pago en CRC para reportes y totales de caja (tolerante a datos históricos).
     * <ul>
     *   <li>Moneda CRC o nula → monto. La decisión se toma por la moneda del pago, nunca por el tipo
     *       de cambio: un pago en CRC sobre factura USD guarda la tasa del USD pero ya está en colones.</li>
     *   <li>Moneda extranjera con tipo de cambio &gt; 0 → monto × tipoCambio (HALF_UP, 2 decimales).</li>
     *   <li>Moneda extranjera sin tipo de cambio (filas anteriores a ADR-011) → monto nominal.</li>
     * </ul>
     * Un monto nulo se toma como 0.
     */
    public static BigDecimal montoEnCrcReporte(BigDecimal monto, MonedaFE monedaPago, BigDecimal tipoCambio) {
        if (monto == null) {
            return BigDecimal.ZERO;
        }
        if (oCrc(monedaPago) == MonedaFE.CRC || tipoCambio == null || tipoCambio.signum() <= 0) {
            return monto;
        }
        return monto.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validarTipoCambio(BigDecimal tipoCambio) {
        if (tipoCambio == null || tipoCambio.signum() <= 0) {
            throw new IllegalArgumentException("El tipo de cambio de la operación debe ser mayor a 0");
        }
    }
}
