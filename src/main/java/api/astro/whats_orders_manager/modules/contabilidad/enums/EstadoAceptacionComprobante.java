package api.astro.whats_orders_manager.modules.contabilidad.enums;

/**
 * Estado de aceptación del comprobante electrónico del proveedor con mensaje receptor (T-054, ADR-067 §4).
 * Una CxP nace PENDIENTE; sin aceptación no hay crédito fiscal (ADR-065).
 */
public enum EstadoAceptacionComprobante {
    PENDIENTE,
    ACEPTADO,
    RECHAZADO
}
