package api.astro.whats_orders_manager.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para transferir datos de plantillas WhatsApp entre capas
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaWhatsAppDTO {
    
    private Integer idPlantilla;
    private String nombre;
    private String codigoMeta;
    private String categoria; // UTILITY, MARKETING, AUTHENTICATION
    private String idioma;
    private String contenido;
    private List<String> parametros;
    private String estadoMeta; // PENDING, APPROVED, REJECTED
    private String templateId;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaActualizacion;
    
    /**
     * Verifica si la plantilla está lista para usar
     */
    public boolean estaListaParaUsar() {
        return Boolean.TRUE.equals(activo) && "APPROVED".equals(estadoMeta);
    }
    
    /**
     * Verifica si la plantilla está aprobada por Meta
     */
    public boolean estaAprobada() {
        return "APPROVED".equals(estadoMeta);
    }
    
    /**
     * Verifica si la plantilla está pendiente de aprobación
     */
    public boolean estaPendiente() {
        return "PENDING".equals(estadoMeta);
    }
    
    /**
     * Verifica si la plantilla fue rechazada
     */
    public boolean estaRechazada() {
        return "REJECTED".equals(estadoMeta);
    }
    
    /**
     * Obtiene el número de parámetros de la plantilla
     */
    public int getNumeroParametros() {
        return parametros != null ? parametros.size() : 0;
    }
}
