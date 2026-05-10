package api.astro.whats_orders_manager.modules.configuracion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para actividad económica desde API de Hacienda Costa Rica.
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
public class HaciendaActividadDTO {
    
    /**
     * Estado de la actividad (A=Activa, I=Inactiva)
     */
    private String estado;
    
    /**
     * Tipo de actividad (P=Principal, S=Secundaria)
     */
    private String tipo;
    
    /**
     * Código de actividad económica (CAECR)
     */
    private String codigo;
    
    /**
     * Descripción de la actividad
     */
    private String descripcion;
    
    /**
     * Clasificación CIIU versión 3
     */
    private List<HaciendaCIIUDTO> ciiu3;
}
