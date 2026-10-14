package api.astro.whats_orders_manager.modules.proveedores.dto;

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
public class OrdenCompraDTO {

    private Long proveedorId;
    private LocalDate fechaEntregaEsperada;
    private MonedaFE moneda;
    /** Tipo de cambio (ADR-011). Null → tasa de venta registrada para la fecha de la orden. */
    private BigDecimal tipoCambio;
    private BigDecimal impuesto;
    private String notas;
    private List<DetalleOrdenCompraDTO> detalles;
}
