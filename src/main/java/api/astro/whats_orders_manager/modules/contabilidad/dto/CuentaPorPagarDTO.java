package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagarDTO {

    private Long proveedorId;
    private Long ordenCompraId;
    private String descripcion;
    /**
     * Total de la CxP con orden de compra (debe coincidir con el de la OC). En la CxP manual se ignora: subtotal, impuesto y
     * total se calculan a partir de las líneas (T-054).
     */
    private BigDecimal monto;
    /** Se ignoran (T-054, ADR-067): los totales de la CxP manual salen de las líneas. */
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private MonedaFE moneda;
    /** Tipo de cambio (ADR-011). Null → tasa de venta registrada para la fecha de emisión. */
    private BigDecimal tipoCambio;
    /**
     * En una CxP manual la fecha de emisión es la del comprobante del proveedor (ADR-014, ADR-066): se toma de
     * {@link #fechaComprobante} y este campo se ignora. En la creación desde una OC no se usa.
     */
    private LocalDate fechaEmision;
    /** Número o consecutivo del comprobante del proveedor (T-052). Obligatorio en la CxP manual. */
    private String numeroComprobanteProveedor;
    /** Clave numérica de 50 dígitos del comprobante del proveedor; opcional. */
    private String claveComprobante;
    /** Fecha de emisión del comprobante del proveedor; obligatoria en la CxP manual y no futura. */
    private LocalDate fechaComprobante;
    /** Líneas de la CxP manual (T-054, ADR-067): al menos una, cada una con su cuenta, tarifa, base, IVA y tratamiento. */
    private List<CuentaPorPagarLineaDTO> lineas;
    private LocalDate fechaVencimiento;
    private String notas;
}
