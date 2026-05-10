package api.astro.whats_orders_manager.modules.configuracion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta de búsqueda de códigos CABYS desde API de Hacienda.
 * Endpoint: https://api.hacienda.go.cr/fe/cabys?q={busqueda}&top={cantidad}
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CabysBusquedaDTO {
    
    /**
     * Total de resultados encontrados
     */
    private Integer total;
    
    /**
     * Cantidad de resultados devueltos (limitado por 'top')
     */
    private Integer cantidad;
    
    /**
     * Lista de códigos CABYS encontrados
     */
    private List<CabysDTO> cabys;
    
    /**
     * Indica si la búsqueda fue exitosa (para control interno)
     */
    private Boolean exitosa;
    
    /**
     * Mensaje de error si la búsqueda falló
     */
    private String mensajeError;
    
    /**
     * Verifica si hay resultados
     * 
     * @return true si encontró al menos un resultado
     */
    public boolean tieneResultados() {
        return cabys != null && !cabys.isEmpty();
    }
    
    /**
     * Obtiene el primer resultado (más relevante)
     * 
     * @return Primer código CABYS o null
     */
    public CabysDTO getPrimerResultado() {
        if (!tieneResultados()) {
            return null;
        }
        return cabys.get(0);
    }
}
