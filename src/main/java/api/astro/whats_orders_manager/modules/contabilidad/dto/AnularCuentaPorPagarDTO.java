package api.astro.whats_orders_manager.modules.contabilidad.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cuerpo del POST de anulación de una cuenta por pagar. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnularCuentaPorPagarDTO {
    /** Motivo de la anulación, de 3 a 180 caracteres. */
    private String motivo;
}
