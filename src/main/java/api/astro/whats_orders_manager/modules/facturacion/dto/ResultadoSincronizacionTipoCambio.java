package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSincronizacionTipoCambio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resultado de sincronizar el tipo de cambio de una moneda (T-008).
 * fecha/tasaCompra/tasaVenta son null cuando el proveedor falló.
 */
public record ResultadoSincronizacionTipoCambio(
        String moneda,
        EstadoSincronizacionTipoCambio estado,
        LocalDate fecha,
        BigDecimal tasaCompra,
        BigDecimal tasaVenta,
        String mensaje
) {
}
