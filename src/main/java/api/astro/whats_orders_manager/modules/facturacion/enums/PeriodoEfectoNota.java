package api.astro.whats_orders_manager.modules.facturacion.enums;

/**
 * Periodo en el que una nota de crédito o débito refleja su efecto contable (nota al pie de la nota 9 del Anexo v4.4,
 * ADR-037 §4).
 */
public enum PeriodoEfectoNota {
    /** Periodo en que se genera la nota: el asiento lleva la fecha de emisión de la nota. */
    NOTA,
    /** Periodo del comprobante que se modifica: el asiento lleva la fecha del asiento de venta original. */
    DOCUMENTO_ORIGINAL
}
