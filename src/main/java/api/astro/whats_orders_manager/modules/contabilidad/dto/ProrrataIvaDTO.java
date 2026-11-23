package api.astro.whats_orders_manager.modules.contabilidad.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Alta o corrección del porcentaje provisional de prorrata del IVA de un año (T-054). En la corrección el año sale de la ruta. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProrrataIvaDTO {
    private Integer anio;
    /** Entre 0 y 100, con hasta 2 decimales. */
    private BigDecimal porcentaje;
    private String nota;
}
