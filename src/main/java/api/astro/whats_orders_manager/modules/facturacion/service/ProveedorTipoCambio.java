package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.TasaDelDia;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.exception.ProveedorTipoCambioException;

/**
 * Fuente automática del tipo de cambio del día (ADR-004 / ADR-011).
 *
 * Implementación actual: {@code HaciendaTipoCambioClient} (API pública de Hacienda, fuente
 * {@link FuenteTipoCambio#API_EXTERNA}). Queda preparada para una futura implementación contra el
 * servicio web SDDE del BCCR (fuente {@link FuenteTipoCambio#BCCR}).
 */
public interface ProveedorTipoCambio {

    /**
     * Tipo de cambio vigente publicado hoy por el proveedor para la moneda.
     *
     * @param moneda moneda extranjera (USD, EUR)
     * @return fecha informada por el proveedor y tasas de compra/venta (&gt; 0)
     * @throws ProveedorTipoCambioException si el proveedor falla, la respuesta es inválida
     *                                      o la moneda no está soportada
     */
    TasaDelDia obtenerTasaDelDia(MonedaFE moneda);

    /** Fuente con la que se registran las tasas de este proveedor. */
    FuenteTipoCambio getFuente();
}
