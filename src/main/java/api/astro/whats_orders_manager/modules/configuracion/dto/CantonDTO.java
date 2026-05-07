package api.astro.whats_orders_manager.modules.configuracion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de Cantón
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CantonDTO {
    private String provinciaCodigo;
    private String codigo;
    private String nombre;
    private String codigoCompleto; // Formato: "1-01"
}
