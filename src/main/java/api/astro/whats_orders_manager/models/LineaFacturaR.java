package api.astro.whats_orders_manager.models;


import java.math.BigDecimal;
import java.sql.Timestamp;

//id_producto, cantidad, precio_unitario, subtotal, create_by, update_by, create_date, update_date, id_linea_factura
public record LineaFacturaR(
        Integer id_linea_factura,
        Integer numero_linea,
        Integer id_producto,
        Integer id_factura,
        String descripcion,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        Integer create_by,
        Integer update_by,
        Timestamp create_date,
        Timestamp update_date
) {
    public LineaFacturaR(LineaFactura lineaFactura) {
        this(
                lineaFactura.getIdLineaFactura(),
                lineaFactura.getNumeroLinea(),
                lineaFactura.getProducto().getIdProducto(),
                lineaFactura.getFactura().getIdFactura(),
                lineaFactura.getProducto().getDescripcion(),
                lineaFactura.getCantidad(),
                lineaFactura.getPrecioUnitario(),
                lineaFactura.getSubtotal(),
                lineaFactura.getCreateBy(),
                lineaFactura.getUpdateBy(),
                lineaFactura.getCreateDate(),
                lineaFactura.getUpdateDate()
        );
    }
}