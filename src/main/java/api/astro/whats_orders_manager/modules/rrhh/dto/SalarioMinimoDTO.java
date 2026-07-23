package api.astro.whats_orders_manager.modules.rrhh.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SalarioMinimoDTO {

    private Long id;
    private String categoria;
    private String descripcionCategoria;
    private BigDecimal montoMensual;
    private LocalDate vigenciaDesde;
    private LocalDate vigenciaHasta;
}
