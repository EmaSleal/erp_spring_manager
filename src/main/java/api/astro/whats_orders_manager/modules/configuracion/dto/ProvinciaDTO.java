package api.astro.whats_orders_manager.modules.configuracion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de Provincia
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinciaDTO {
    private String codigo;
    private String nombre;
}
