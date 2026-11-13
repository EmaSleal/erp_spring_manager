package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

import java.util.List;

/**
 * La factura tiene productos sin código CAByS: no se emite su comprobante (T-033). Se lanza antes de tomar el
 * consecutivo y generar la clave, así que no consume numeración ni crea comprobante. Se responde 409.
 */
public class ProductosSinCabysException extends RuntimeException {

    private final List<String> productos;

    public ProductosSinCabysException(List<String> productos) {
        super("No se puede emitir el comprobante electrónico: estos productos no tienen código CAByS: "
            + String.join(", ", productos) + ". Regístrelo en cada producto y vuelva a intentarlo.");
        this.productos = List.copyOf(productos);
    }

    public List<String> getProductos() {
        return productos;
    }
}
