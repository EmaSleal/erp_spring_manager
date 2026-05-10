package api.astro.whats_orders_manager.modules.facturacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para líneas de factura.
 * Contiene los datos del producto sin referencias circulares.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since 2026-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaFacturaDTO {
    
    private Integer idLineaFactura;
    private Integer numeroLinea;
    
    // Información del producto
    private Integer idProducto;
    private String codigoProducto;
    private String descripcionProducto;
    
    // Cantidades y precios
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
