package api.astro.whats_orders_manager.modules.rrhh.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartamentoDTO {

    private Long id;
    private String nombre;
    private boolean activo;
    private Long jefeId;
    /** Read-only display field — populated when loading; not used on save. */
    private String jefeNombre;
}
