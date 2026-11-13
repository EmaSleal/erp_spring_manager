package api.astro.whats_orders_manager.modules.facturacion.electronica.event;

import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;

/**
 * El comprobante de una nota de crédito o débito quedó ACEPTADO o RECHAZADO por Hacienda (ADR-037 §3). Se publica de
 * forma síncrona dentro de la transacción que guarda el estado: los efectos de la nota se aplican en esa misma
 * transacción.
 */
public record ComprobanteNotaResueltoEvent(ComprobanteElectronico comprobante) {
}
