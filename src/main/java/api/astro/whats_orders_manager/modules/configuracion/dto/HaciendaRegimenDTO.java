package api.astro.whats_orders_manager.modules.configuracion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para régimen tributario desde API de Hacienda Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HaciendaRegimenDTO {
    
    /**
     * Código del régimen (1=General, 2=Simplificado, 3=No contribuyente)
     */
    private Integer codigo;
    
    /**
     * Descripción del régimen
     */
    private String descripcion;
}
