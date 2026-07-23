package api.astro.whats_orders_manager.modules.rrhh.dto;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PuestoDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal salarioBase;
    private String categoriaSalarialMinima;
    private TipoJornada tipoJornada;
    private Long departamentoId;
    private String departamentoNombre;
    private boolean activo;
}
