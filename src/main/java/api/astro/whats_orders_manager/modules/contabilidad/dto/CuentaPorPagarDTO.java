package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagarDTO {

    private Long proveedorId;
    private Long ordenCompraId;
    private String descripcion;
    /** Total de la CxP. En una CxP manual, si viene null se calcula como subtotal + impuesto. */
    private BigDecimal monto;
    /** Desglose de la CxP manual (ADR-016); se ignora si viene ordenCompraId. */
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    /** Contrapartida de la CxP manual (ADR-016); se ignora si viene ordenCompraId. */
    private Long cuentaGastoId;
    private MonedaFE moneda;
    /** Tipo de cambio (ADR-011). Null → tasa de venta registrada para la fecha de emisión. */
    private BigDecimal tipoCambio;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String notas;
}
