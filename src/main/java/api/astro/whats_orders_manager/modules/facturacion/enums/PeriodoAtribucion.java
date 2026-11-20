package api.astro.whats_orders_manager.modules.facturacion.enums;

/**
 * Mes al que se atribuye el IVA de una nota de crédito o débito, o de una factura sustituta (ADR-050 §2). Decide solo
 * el período fiscal de atribución, no la fecha del asiento: esa es siempre la fecha del documento (ADR-050 §1).
 */
public enum PeriodoAtribucion {
    /** Mes de emisión del documento. */
    EMISION,
    /** Mes de la factura original (fecha de emisión de su comprobante ACEPTADO). */
    ORIGINAL
}
