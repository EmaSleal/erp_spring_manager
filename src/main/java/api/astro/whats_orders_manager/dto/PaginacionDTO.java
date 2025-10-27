package api.astro.whats_orders_manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO genérico para resultados paginados
 * Encapsula la información de paginación y los datos
 * 
 * @param <T> Tipo de entidad contenida en la página
 * @version 1.0
 * @since 26/10/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginacionDTO<T> {
    
    /**
     * Lista de elementos de la página actual
     */
    private List<T> contenido;
    
    /**
     * Número de página actual (0-indexed)
     */
    private int paginaActual;
    
    /**
     * Tamaño de página (elementos por página)
     */
    private int tamanoPagina;
    
    /**
     * Total de elementos en todas las páginas
     */
    private long totalElementos;
    
    /**
     * Total de páginas disponibles
     */
    private int totalPaginas;
    
    /**
     * Indica si existe una página siguiente
     */
    public boolean tieneSiguiente() {
        return paginaActual < totalPaginas - 1;
    }
    
    /**
     * Indica si existe una página anterior
     */
    public boolean tieneAnterior() {
        return paginaActual > 0;
    }
    
    /**
     * Indica si es la primera página
     */
    public boolean esPrimera() {
        return paginaActual == 0;
    }
    
    /**
     * Indica si es la última página
     */
    public boolean esUltima() {
        return paginaActual == totalPaginas - 1;
    }
}
