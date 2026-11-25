package api.astro.whats_orders_manager.modules.regimen.dto;

import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Cuerpo de alta y corrección de un tramo de régimen (T-056). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegimenTributarioDTO {
    private Regimen regimen;
    private LocalDate vigenteDesde;
    private String nota;
}
