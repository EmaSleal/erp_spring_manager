package api.astro.whats_orders_manager.modules.nomina.dto;

import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full Nomina DTO — header plus all detail lines.
 * Used for single-record endpoints (GET /{id}, calcular, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NominaDTO {

    private Long id;
    private String numero;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private LocalDate fechaPago;
    private TipoNomina tipo;
    private EstadoNomina estado;
    private BigDecimal totalBruto;
    private BigDecimal totalNeto;
    private int cantidadEmpleados;

    private List<DetalleNominaDTO> detalles;
}
