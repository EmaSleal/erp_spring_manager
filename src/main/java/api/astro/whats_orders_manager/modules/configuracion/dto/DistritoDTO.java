package api.astro.whats_orders_manager.modules.configuracion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de Distrito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistritoDTO {
    private String provinciaCodigo;
    private String cantonCodigo;
    private String codigo;
    private String nombre;
    private String codigoCompleto; // Formato: "1-01-01"
}
