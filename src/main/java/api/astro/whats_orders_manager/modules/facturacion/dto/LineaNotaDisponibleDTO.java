package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;

/**
 * Una línea de la factura con lo que aún admite acreditar una nota parcial (T-047, ADR-051 §3). Montos con IVA
 * incluido, en la moneda de la factura; los calcula el servidor.
 */
public record LineaNotaDisponibleDTO(Integer idLineaFactura, Integer numeroLinea, String descripcion, BigDecimal total,
                                     BigDecimal acreditado, BigDecimal disponible) {
}
