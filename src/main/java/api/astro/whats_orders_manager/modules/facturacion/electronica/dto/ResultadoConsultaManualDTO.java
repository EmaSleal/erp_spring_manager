package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

/**
 * Resultado de "Consultar estado en Hacienda": el comprobante tras aplicar la respuesta y un mensaje para el usuario
 * (p. ej. por qué un 404 todavía no cambió el estado).
 */
public record ResultadoConsultaManualDTO(ComprobanteElectronicoDTO comprobante, String mensaje) {
}
