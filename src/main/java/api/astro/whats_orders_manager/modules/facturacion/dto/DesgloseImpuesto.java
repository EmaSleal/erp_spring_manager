package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;

/**
 * Desglose de IVA de un importe (ADR-009).
 *
 * @param base     Monto sin impuesto (2 decimales)
 * @param impuesto Monto del impuesto (2 decimales)
 * @param tarifa   Tarifa aplicada en porcentaje (0 si exento / no gravado)
 */
public record DesgloseImpuesto(BigDecimal base, BigDecimal impuesto, BigDecimal tarifa) {

    public static final DesgloseImpuesto CERO =
            new DesgloseImpuesto(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO);

    /** Importe con impuesto: base + impuesto. */
    public BigDecimal total() {
        return base.add(impuesto);
    }

    /** true si la línea lleva impuesto (tarifa mayor que cero). */
    public boolean gravado() {
        return tarifa != null && tarifa.signum() > 0;
    }

    /** Suma de dos desgloses (la tarifa resultante solo se conserva si coincide). */
    public DesgloseImpuesto sumar(DesgloseImpuesto otro) {
        BigDecimal tarifaSuma = tarifa != null && otro.tarifa != null && tarifa.compareTo(otro.tarifa) == 0
                ? tarifa : null;
        return new DesgloseImpuesto(base.add(otro.base), impuesto.add(otro.impuesto), tarifaSuma);
    }
}
