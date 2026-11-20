package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido de POST /api/facturas/notas-credito (T-032, T-047).
 *
 * @param idFactura        factura a la que se emite la nota
 * @param tipo             CREDITO (por defecto si se omite); DEBITO responde 400
 * @param codigoReferencia código de la nota 9: 01 o 13 (nota total) o 02 o 14 (nota parcial)
 * @param razon            razón de la referencia, de 3 a 180 caracteres
 * @param lineas           solo en la nota parcial (02 y 14): líneas de la factura y monto a acreditar de cada una; en
 *                         la nota total (01 y 13) se omite
 */
public record EmitirNotaCreditoRequest(Integer idFactura, TipoNotaFactura tipo, String codigoReferencia,
                                       String razon, List<LineaAcreditar> lineas) {

    /** Pedido de una nota total (sin líneas). */
    public EmitirNotaCreditoRequest(Integer idFactura, TipoNotaFactura tipo, String codigoReferencia, String razon) {
        this(idFactura, tipo, codigoReferencia, razon, null);
    }

    /**
     * @param idLineaFactura id de la línea de la factura (LineaFactura.idLineaFactura)
     * @param montoAcreditar monto a acreditar con IVA incluido, en la moneda de la factura: mayor que 0, dos decimales
     */
    public record LineaAcreditar(Integer idLineaFactura, BigDecimal montoAcreditar) {
    }
}
