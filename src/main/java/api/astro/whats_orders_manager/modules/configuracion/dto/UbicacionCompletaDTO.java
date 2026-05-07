package api.astro.whats_orders_manager.modules.configuracion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta completa de ubicación (con provincia, cantón y distrito)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionCompletaDTO {
    private String provinciaCodigo;
    private String provinciaNombre;
    private String cantonCodigo;
    private String cantonNombre;
    private String distritoCodigo;
    private String distritoNombre;
    private String codigoCompleto; // Formato: "1-01-01"
    private String ubicacionCompleta; // Formato: "San José, San José, Carmen"
}
