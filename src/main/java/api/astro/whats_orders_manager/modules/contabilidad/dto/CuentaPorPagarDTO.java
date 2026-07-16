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
    private BigDecimal monto;
    private MonedaFE moneda;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String notas;
}
