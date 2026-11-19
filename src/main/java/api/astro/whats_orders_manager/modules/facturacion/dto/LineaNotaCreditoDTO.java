package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;

/** Línea de la nota de crédito: la foto de la línea de la factura. */
public record LineaNotaCreditoDTO(Integer numeroLinea, String producto, String descripcion, String codigoCabys,
                                  BigDecimal cantidad, String unidadMedida, BigDecimal precioUnitario,
                                  BigDecimal descuento, BigDecimal tarifaIva, BigDecimal montoIva,
                                  BigDecimal subtotal, BigDecimal total) {
}
