package api.astro.whats_orders_manager.modules.proveedores.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vista de una orden de compra para facturar (T-053, ADR-066 §4): total, facturado (CxP no anuladas), pendiente de facturar,
 * exceso (lo facturado por encima del total), sus CxP, sus detalles y la cuenta propuesta para las líneas de mercadería
 * (parámetro COMPRAS_MERCADERIA; nula si no está configurado). Todo en la moneda de la orden.
 */
public record OrdenCompraFacturacionDTO(
        Long ordenCompraId,
        String numero,
        Long proveedorId,
        String proveedorNombre,
        String estado,
        boolean admiteFacturar,
        String moneda,
        BigDecimal total,
        BigDecimal facturado,
        BigDecimal pendiente,
        BigDecimal exceso,
        Long cuentaPropuestaId,
        String cuentaPropuestaCodigo,
        String cuentaPropuestaNombre,
        List<Detalle> detalles,
        List<Cuenta> cuentas) {

    public record Detalle(Long id, String descripcion, Integer cantidad, BigDecimal precioUnitario, BigDecimal total) {
    }

    public record Cuenta(Long id, String numero, String numeroComprobanteProveedor, LocalDate fechaComprobante,
                         BigDecimal monto, String estado, String estadoAceptacion) {
    }
}
