package api.astro.whats_orders_manager.modules.configuracion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para situación tributaria desde API de Hacienda Costa Rica.
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
public class HaciendaSituacionDTO {
    
    /**
     * Indica si está moroso (SI/NO)
     */
    private String moroso;
    
    /**
     * Indica si está omiso (SI/NO)
     */
    private String omiso;
    
    /**
     * Estado tributario (Inscrito, Suspendido, Cancelado)
     */
    private String estado;
    
    /**
     * Administración tributaria que lo gestiona
     */
    private String administracionTributaria;
}
