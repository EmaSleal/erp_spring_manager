package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Datos de un producto que van en una LineaDetalle del XML (CAByS, descripción, unidad de medida). Una sola
 * resolución para el XML de factura, tiquete y nota de crédito, la foto de líneas de la nota y el PDF.
 *
 * El CAByS vive solo en el artículo maestro (ADR-040): un producto sin maestro, o con maestro sin CAByS, no tiene
 * CAByS aunque Producto.codigoCabys (deprecado) tenga valor y no se factura electrónicamente
 * ({@link #productosSinCabys}; T-033, ADR-024, ADR-026). La unidad y el Detalle siguen ADR-041.
 */
public final class DatosFeProducto {

    /** Unidad de medida cuando la presentación no define un código FE. */
    public static final String UNIDAD_POR_DEFECTO = "Unid";

    /** maxLength de LineaDetalle/Detalle en el XSD v4.4 (una prueba lo compara con el XSD). */
    public static final int DETALLE_MAX = 200;

    private DatosFeProducto() {
    }

    /** CAByS del producto: solo el del artículo maestro (ADR-040). Null si no hay maestro o no tiene código. */
    public static String codigoCabys(Producto producto) {
        ArticuloMaestro maestro = producto.getArticuloMaestro();
        String cabys = maestro != null ? maestro.getCodigoCabys() : null;
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

    /**
     * Descripción del artículo maestro. Sin maestro se usa la de Producto solo como texto para mostrar (mensajes, PDF);
     * el CAByS nunca tiene respaldo en Producto.
     */
    @SuppressWarnings("deprecation")
    public static String descripcion(Producto producto) {
        ArticuloMaestro maestro = producto.getArticuloMaestro();
        return maestro != null ? maestro.getDescripcion() : producto.getDescripcion();
    }

    /** Empacada: se vende por envase (null cuenta como empacada, ADR-041). Sin presentación: false. */
    private static boolean empacada(Presentacion presentacion) {
        return presentacion != null && !Boolean.FALSE.equals(presentacion.getEmpacado());
    }

    /** 'Unid' si la presentación es empacada; si no, su codigo_unidad_fe. Sin presentación o sin código: 'Unid'. */
    public static String unidadMedida(Producto producto) {
        Presentacion presentacion = producto.getPresentacion();
        if (presentacion != null && !empacada(presentacion)) {
            String codigo = presentacion.getCodigoUnidadFE();
            if (codigo != null && !codigo.isBlank()) {
                return codigo.trim();
            }
        }
        return UNIDAD_POR_DEFECTO;
    }

    /**
     * Detalle de la línea: «descripción presentación» si la presentación es empacada; solo la descripción en otro
     * caso. Se recorta a {@link #DETALLE_MAX}; si no cabe todo, se recorta la descripción y se conserva la
     * presentación (es lo que distingue las líneas).
     */
    public static String detalle(Producto producto) {
        String descripcion = descripcion(producto);
        descripcion = descripcion != null ? descripcion.trim() : "";
        Presentacion presentacion = producto.getPresentacion();
        String nombre = empacada(presentacion) && presentacion.getNombre() != null ? presentacion.getNombre().trim() : "";
        if (nombre.isEmpty()) {
            return recortar(descripcion, DETALLE_MAX);
        }
        if (descripcion.length() + 1 + nombre.length() <= DETALLE_MAX) {
            return descripcion.isEmpty() ? nombre : descripcion + " " + nombre;
        }
        int espacio = DETALLE_MAX - 1 - nombre.length();
        return espacio > 0 ? recortar(descripcion, espacio).stripTrailing() + " " + nombre
            : recortar(descripcion + " " + nombre, DETALLE_MAX);
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
