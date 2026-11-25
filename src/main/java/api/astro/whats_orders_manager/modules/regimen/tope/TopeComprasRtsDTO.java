package api.astro.whats_orders_manager.modules.regimen.tope;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Alta o corrección de los parámetros del tope de un año (T-056). En la corrección el año sale de la ruta. Multiplicador y
 * umbrales en null: al crear, quedan los valores por defecto de la columna; al corregir, se conservan los actuales.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopeComprasRtsDTO {
    private Integer anio;
    private BigDecimal salarioBase;
    private BigDecimal multiplicador;
    private BigDecimal umbralAviso;
    private BigDecimal umbralTope;
    private String nota;
}
