package api.astro.whats_orders_manager.modules.facturacion.enums;

/**
 * Estado de una nota sobre una factura (ADR-037 §3). Sus efectos (asiento, factura anulada) se aplican solo cuando
 * Hacienda acepta su comprobante.
 */
public enum EstadoNotaFactura {
    /** Emitida; su comprobante todavía no está ACEPTADO ni RECHAZADO. */
    EN_TRAMITE,
    /** Hacienda la aceptó y sus efectos se aplicaron. */
    APLICADA,
    /** Hacienda la rechazó: sin efectos; se puede emitir otra. */
    RECHAZADA
}
