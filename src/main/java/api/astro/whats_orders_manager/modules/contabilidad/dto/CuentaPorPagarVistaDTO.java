package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagarIva;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TarifaIva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuenta por pagar tal como la devuelve la API JSON (T-052, P-023 §1): datos planos, sin entidades ni proxies
 * perezosos. Se arma dentro de la transacción de lectura del servicio.
 *
 * {@code fechaEmisionComprobante} es la fecha de emisión del comprobante del proveedor (null en las CxP anteriores a
 * V53, que no la guardaban); {@code fechaEmision} es la de la CxP, que en las CxP manuales nuevas es la misma (ADR-014).
 */
public record CuentaPorPagarVistaDTO(
        Long id,
        String numero,
        Long proveedorId,
        String proveedorNombre,
        Long ordenCompraId,
        String ordenCompraNumero,
        String descripcion,
        BigDecimal monto,
        BigDecimal saldoPendiente,
        BigDecimal subtotal,
        BigDecimal impuesto,
        String moneda,
        BigDecimal tipoCambio,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String estado,
        String numeroComprobanteProveedor,
        String claveComprobante,
        LocalDate fechaEmisionComprobante,
        List<DesgloseIva> desglose,
        Long cuentaGastoId,
        Integer anuladaPorId,
        LocalDateTime fechaAnulacion,
        String motivoAnulacion,
        String notas) {

    /** Fila del desglose de IVA; {@code codigoTarifa} y {@code porcentaje} son null cuando es «sin tarifa». */
    public record DesgloseIva(String codigoTarifa, BigDecimal porcentaje, BigDecimal base, BigDecimal iva) {

        static DesgloseIva de(CuentaPorPagarIva fila) {
            BigDecimal porcentaje = TarifaIva.deCodigo(fila.getCodigoTarifa()).map(TarifaIva::getPorcentaje).orElse(null);
            return new DesgloseIva(fila.getCodigoTarifa(), porcentaje, fila.getBase(), fila.getIva());
        }
    }

    /** Debe llamarse con la entidad adjunta a una sesión abierta (proveedor, OC y desglose son perezosos). */
    public static CuentaPorPagarVistaDTO de(CuentaPorPagar c) {
        var proveedor = c.getProveedor();
        var oc = c.getOrdenCompra();
        return new CuentaPorPagarVistaDTO(
            c.getId(), c.getNumero(),
            proveedor != null ? proveedor.getId() : null,
            proveedor != null ? proveedor.getNombre() : null,
            oc != null ? oc.getId() : null,
            oc != null ? oc.getNumero() : null,
            c.getDescripcion(), c.getMonto(), c.getSaldoPendiente(), c.getSubtotal(), c.getImpuesto(),
            c.getMoneda() != null ? c.getMoneda().name() : "CRC",
            c.getTipoCambio(), c.getFechaEmision(), c.getFechaVencimiento(),
            c.getEstado() != null ? c.getEstado().name() : null,
            c.getNumeroComprobanteProveedor(), c.getClaveComprobante(), c.getFechaComprobante(),
            c.getDesglose().stream().map(DesgloseIva::de).toList(),
            c.getCuentaGastoId(), c.getAnuladaPorId(), c.getFechaAnulacion(), c.getMotivoAnulacion(), c.getNotas());
    }
}
