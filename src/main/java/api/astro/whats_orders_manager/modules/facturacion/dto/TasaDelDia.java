package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Tipo de cambio publicado por un proveedor externo para un día (ADR-004 / ADR-011):
 * colones (CRC) por una unidad de la moneda extranjera.
 *
 * @param fecha  fecha que informa el proveedor (no la del servidor)
 * @param compra tasa de compra, &gt; 0
 * @param venta  tasa de venta, &gt; 0
 */
public record TasaDelDia(LocalDate fecha, BigDecimal compra, BigDecimal venta) {

    public TasaDelDia {
        Objects.requireNonNull(fecha, "La fecha del tipo de cambio es obligatoria");
        if (compra == null || compra.signum() <= 0) {
            throw new IllegalArgumentException("La tasa de compra debe ser mayor a 0: " + compra);
        }
        if (venta == null || venta.signum() <= 0) {
            throw new IllegalArgumentException("La tasa de venta debe ser mayor a 0: " + venta);
        }
    }
}
