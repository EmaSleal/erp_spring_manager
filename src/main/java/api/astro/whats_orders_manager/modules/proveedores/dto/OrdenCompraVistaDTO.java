package api.astro.whats_orders_manager.modules.proveedores.dto;

import api.astro.whats_orders_manager.modules.proveedores.model.DetalleOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vista JSON de una orden de compra (T-053, revisión 1): campos planos armados dentro de una transacción de lectura, de
 * modo que ninguna relación diferida (proveedor, detalles, producto) llega al serializador.
 */
public record OrdenCompraVistaDTO(
        Long id,
        String numero,
        Long proveedorId,
        String proveedorNombre,
        String estado,
        LocalDate fecha,
        LocalDate fechaEntregaEsperada,
        LocalDate fechaRecepcion,
        String moneda,
        BigDecimal tipoCambio,
        BigDecimal subtotal,
        BigDecimal impuesto,
        BigDecimal total,
        String notas,
        List<Detalle> detalles) {

    public record Detalle(Long id, Integer productoId, String productoCodigo, String productoNombre, String descripcion, Integer cantidad,
                          Integer cantidadRecibida, BigDecimal precioUnitario, BigDecimal total) {

        static Detalle de(DetalleOrdenCompra d) {
            return new Detalle(d.getId(), d.getProducto() != null ? d.getProducto().getIdProducto() : null,
                d.getProducto() != null ? d.getProducto().getCodigo() : null,
                d.getProducto() != null ? d.getProducto().getDescripcion() : null, d.getDescripcion(), d.getCantidad(),
                d.getCantidadRecibida(), d.getPrecioUnitario(), d.getTotal());
        }
    }

    /** Debe llamarse con la sesión abierta (transacción de lectura). */
    public static OrdenCompraVistaDTO de(OrdenCompra oc) {
        return new OrdenCompraVistaDTO(oc.getId(), oc.getNumero(), oc.getProveedor().getId(),
            oc.getProveedor().getNombre(), oc.getEstado().name(), oc.getFecha(), oc.getFechaEntregaEsperada(),
            oc.getFechaRecepcion(), oc.getMoneda() != null ? oc.getMoneda().getCodigo() : null, oc.getTipoCambio(),
            oc.getSubtotal(), oc.getImpuesto(), oc.getTotal(), oc.getNotas(),
            oc.getDetalles().stream().map(Detalle::de).toList());
    }
}
