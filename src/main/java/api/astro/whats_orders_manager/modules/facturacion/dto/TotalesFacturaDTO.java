package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;

import java.math.BigDecimal;

/**
 * Totales persistidos de una factura tras guardar sus líneas (T-024). La UI muestra estos valores, no los que
 * calcula el navegador.
 *
 * @param idFactura Factura actualizada
 * @param moneda    Código de moneda de la factura (los montos están en esa moneda)
 * @param subtotal  Σ bases sin IVA
 * @param igv       Σ IVA
 * @param total     Σ importes de línea
 * @param lineas    Cantidad de líneas persistidas
 */
public record TotalesFacturaDTO(Integer idFactura, String moneda, BigDecimal subtotal, BigDecimal igv,
                                BigDecimal total, int lineas) {

    public static TotalesFacturaDTO of(Factura factura) {
        String moneda = factura.getMonedaFE() != null ? factura.getMonedaFE().getCodigo() : null;
        return new TotalesFacturaDTO(factura.getIdFactura(), moneda, factura.getSubtotal(), factura.getIgv(),
                factura.getTotal(), factura.getLineas().size());
    }
}
