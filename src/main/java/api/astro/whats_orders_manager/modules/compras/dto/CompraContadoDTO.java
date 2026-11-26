package api.astro.whats_orders_manager.modules.compras.dto;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos de una compra de contado (T-058, ADR-077 §1-2): lo de una CxP manual (proveedor, comprobante, líneas) más cómo se
 * paga. El monto del pago no se envía: es siempre el total de la CxP.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraContadoDTO {

    private Long proveedorId;
    private String numeroComprobanteProveedor;
    /** Clave numérica de 50 dígitos del comprobante; opcional. */
    private String claveComprobante;
    private LocalDate fechaComprobante;
    /** Opcional: si viene vacía se usa «Compra de contado». */
    private String descripcion;
    /** Null → CRC. */
    private MonedaFE moneda;
    private List<CuentaPorPagarLineaDTO> lineas;
    /** Null → EFECTIVO (caja general). */
    private MetodoPagoProveedor metodoPago;
    /** Obligatoria con cualquier método que no sea EFECTIVO; en EFECTIVO se ignora (el origen es la caja general). */
    private Integer idCuentaBancariaOrigen;
    private String referencia;
    private String notas;
}
