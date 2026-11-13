package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;

/**
 * Pedido de POST /api/facturas/notas-credito (T-032).
 *
 * @param idFactura        factura a la que se emite la nota
 * @param tipo             CREDITO (por defecto si se omite); DEBITO responde 400
 * @param codigoReferencia código de la nota 9 (01 o 13 en esta fase)
 * @param razon            razón de la referencia, de 3 a 180 caracteres
 */
public record EmitirNotaCreditoRequest(Integer idFactura, TipoNotaFactura tipo, String codigoReferencia,
                                       String razon) {
}
