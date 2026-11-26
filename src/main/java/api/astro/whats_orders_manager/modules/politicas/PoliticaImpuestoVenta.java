package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Política de impuesto de venta (ADR-072, ADR-073 §1, ADR-076 §3): decide si una venta lleva IVA según el régimen sellado en
 * el documento de origen (la factura), no según una fecha. En el régimen simplificado la tarifa de cada línea es cero y el
 * total es la suma de los importes al precio del cliente; en el general rige la tarifa del producto.
 *
 * La política no consulta la tabla de tramos: el régimen ya está sellado en la factura (regimen_emisor). Una factura sin
 * régimen (entidad armada fuera del servicio, que ya trae GENERAL por defecto) se trata como GENERAL.
 */
@Component
public class PoliticaImpuestoVenta {

    /** Tarifa de IVA a aplicar a una línea de la factura: 0 en SIMPLIFICADO; {@code tarifaDelProducto} en GENERAL. */
    public BigDecimal tarifaAplicable(Factura factura, BigDecimal tarifaDelProducto) {
        return cobraIva(factura) ? tarifaDelProducto : BigDecimal.ZERO;
    }

    /** Si las ventas de la factura llevan IVA. */
    public boolean cobraIva(Factura factura) {
        return factura == null || factura.getRegimenEmisor() != Regimen.SIMPLIFICADO;
    }
}
