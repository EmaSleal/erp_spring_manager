package api.astro.whats_orders_manager.modules.facturacion.dto;

/** Cuerpo opcional de POST /api/facturas/{id}/sustituta: la razón de la referencia (3 a 180 caracteres). */
public record EmitirSustitutaRequest(String razon) {
}
