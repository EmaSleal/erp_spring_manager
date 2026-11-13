package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Datos de un producto que van en una LineaDetalle del XML (CAByS, descripción, unidad de medida). Una sola
 * resolución para el XML de factura y para la foto de las líneas de una nota de crédito: lee del artículo maestro si
 * existe y, si no, del campo de Producto (XML-1).
 *
 * No hay CAByS por defecto (T-033, ADR-024 y ADR-026): un producto sin CAByS no se factura electrónicamente
 * ({@link #productosSinCabys}).
 */
public final class DatosFeProducto {

    /** Unidad de medida cuando la presentación no define un código FE. */
    public static final String UNIDAD_POR_DEFECTO = "Unid";

    private DatosFeProducto() {
    }

    /** CAByS del producto, o null si no tiene. */
    @SuppressWarnings("deprecation")
    public static String codigoCabys(Producto producto) {
        String cabys = Optional.ofNullable(producto.getArticuloMaestro())
            .map(ArticuloMaestro::getCodigoCabys)
            .orElse(producto.getCodigoCabys());
        return (cabys != null && !cabys.isBlank()) ? cabys.trim() : null;
    }

    /**
     * true si el CAByS es de un servicio. El primer dígito del CAByS separa los bienes (0 a 4, partida arancelaria) de
     * los servicios (5 a 9); el resumen del comprobante reparte sus totales con esta regla (Anexo v4.4, apartados b y c).
     *
     * @throws IllegalArgumentException si el CAByS no empieza con un dígito
     */
    public static boolean esServicio(String cabys) {
        if (cabys == null || cabys.isEmpty() || !Character.isDigit(cabys.charAt(0))) {
            throw new IllegalArgumentException("CAByS inválido: " + cabys);
        }
        return cabys.charAt(0) >= '5';
    }

    /** Descripciones (o ids) de los productos de las líneas que no tienen CAByS, sin repetir y en orden. */
    public static List<String> productosSinCabys(Collection<LineaFactura> lineas) {
        Set<String> sinCabys = new LinkedHashSet<>();
        for (LineaFactura linea : lineas) {
            Producto producto = linea.getProducto();
            if (producto != null && codigoCabys(producto) == null) {
                String descripcion = descripcion(producto);
                sinCabys.add(descripcion != null && !descripcion.isBlank() ? descripcion
                    : "producto " + producto.getIdProducto());
            }
        }
        return List.copyOf(sinCabys);
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
