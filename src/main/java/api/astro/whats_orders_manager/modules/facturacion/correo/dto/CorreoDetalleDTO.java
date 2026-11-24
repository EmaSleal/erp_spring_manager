package api.astro.whats_orders_manager.modules.facturacion.correo.dto;

/**
 * Campo {@code correo} del detalle de la factura y de la nota: lo que la interfaz necesita para «Reenviar comprobante por
 * correo». {@code puedeReenviar}: el usuario tiene COMPROBANTE_REENVIAR_CORREO y el comprobante vigente está ACEPTADO.
 */
public record CorreoDetalleDTO(Long comprobanteId, String estadoComprobante, String destinatarioPorDefecto,
                               boolean puedeReenviar) {
}
