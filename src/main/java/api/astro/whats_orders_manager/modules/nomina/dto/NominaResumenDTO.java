package api.astro.whats_orders_manager.modules.nomina.dto;

import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary projection of a Nomina — no detail lines.
 * Used for listing endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NominaResumenDTO {

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
}
