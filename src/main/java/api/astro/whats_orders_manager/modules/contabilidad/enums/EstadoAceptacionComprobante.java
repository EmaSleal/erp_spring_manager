package api.astro.whats_orders_manager.modules.contabilidad.enums;

/**
 * Estado de aceptación del comprobante electrónico del proveedor con mensaje receptor (T-054, ADR-067 §4).
 * Una CxP de un tramo GENERAL nace PENDIENTE; sin aceptación no hay crédito fiscal (ADR-065).
 */
public enum EstadoAceptacionComprobante {
    PENDIENTE,
    ACEPTADO,
    RECHAZADO,
    /**
     * La CxP tiene fecha de comprobante en un tramo SIMPLIFICADO (T-058, ADR-074 §3): no hay mensaje receptor ni crédito
     * fiscal. Se asigna al crearla, no cuenta como pendiente y no pasa a ACEPTADO ni RECHAZADO.
     */
    NO_APLICA
}
