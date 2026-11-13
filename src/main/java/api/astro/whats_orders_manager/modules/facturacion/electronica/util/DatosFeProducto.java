package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;

import java.util.Optional;

/**
 * Datos de un producto que van en una LineaDetalle del XML (CAByS, descripción, unidad de medida). Una sola
 * resolución para el XML de factura y para la foto de las líneas de una nota de crédito: lee del artículo maestro si
 * existe y, si no, del campo de Producto (XML-1).
 */
public final class DatosFeProducto {

    /** CAByS que se envía cuando el producto no tiene uno registrado. */
    public static final String CABYS_POR_DEFECTO = "8522000000000";

    /** Unidad de medida cuando la presentación no define un código FE. */
    public static final String UNIDAD_POR_DEFECTO = "Unid";

    private DatosFeProducto() {
    }

    /** CAByS del producto, o {@link #CABYS_POR_DEFECTO} si no tiene. */
    @SuppressWarnings("deprecation")
    public static String codigoCabys(Producto producto) {
        String cabys = Optional.ofNullable(producto.getArticuloMaestro())
            .map(ArticuloMaestro::getCodigoCabys)
            .orElse(producto.getCodigoCabys());
        return (cabys != null && !cabys.isEmpty()) ? cabys : CABYS_POR_DEFECTO;
    }

    @SuppressWarnings("deprecation")
    public static String descripcion(Producto producto) {
        return Optional.ofNullable(producto.getArticuloMaestro())
            .map(ArticuloMaestro::getDescripcion)
            .orElse(producto.getDescripcion());
    }

    public static String unidadMedida(Producto producto) {
        if (producto.getPresentacion() != null && producto.getPresentacion().getCodigoUnidadFE() != null) {
            return producto.getPresentacion().getCodigoUnidadFE();
        }
        return UNIDAD_POR_DEFECTO;
    }
}
