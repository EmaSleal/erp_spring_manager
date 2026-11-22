package api.astro.whats_orders_manager.modules.facturacion.enums;

/**
 * Modalidad de una nota de crédito (T-051, ADR-051, ADR-061 §3). Se persiste en la nota al emitirla y es lo único que
 * decide cómo se comporta (topes, elegibilidad, efectos al aceptarse), no el código de referencia.
 *
 * <ul>
 *   <li>{@link #TOTAL}: acredita el total del comprobante aceptado y anula la factura.</li>
 *   <li>{@link #PARCIAL}: acredita un monto por línea (IVA incluido); la factura no se anula.</li>
 * </ul>
 */
public enum ModalidadNota {
    TOTAL,
    PARCIAL
}
