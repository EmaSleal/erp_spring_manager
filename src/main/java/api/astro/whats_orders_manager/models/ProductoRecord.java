package api.astro.whats_orders_manager.models;
// id_producto, nombre, precio_institucional, precio_mayorista
//'1', ' esponja fibra verde gde. (Ud)', '600.00', '500.00'

import java.math.BigDecimal;

public record ProductoRecord(
        Integer id_producto,
        String nombre,
        BigDecimal precio_institucional,
        BigDecimal precio_mayorista
) {
    public ProductoRecord(Producto productos) {
        this(
                productos.getIdProducto(),
                productos.getDescripcion(),
                productos.getPrecioInstitucional(),
                productos.getPrecioMayorista()
        );
    }
}
