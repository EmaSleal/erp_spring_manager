package api.astro.whats_orders_manager.util;

import api.astro.whats_orders_manager.dto.PaginacionDTO;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Utilidad para operaciones de paginación
 * Proporciona métodos para convertir entre Page de Spring y PaginacionDTO
 * y para agregar atributos de paginación al modelo de Thymeleaf
 * 
 * @version 1.0
 * @since 26/10/2025
 */
public class PaginacionUtil {
    
    private PaginacionUtil() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Convierte un Page de Spring Data a un PaginacionDTO
     * 
     * @param page Page de Spring Data
     * @param <T> Tipo de entidad
     * @return PaginacionDTO con la información de la página
     */
    public static <T> PaginacionDTO<T> fromPage(Page<T> page) {
        return new PaginacionDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
    
    /**
     * Crea un PaginacionDTO a partir de una lista paginada manualmente
     * 
     * @param contenido Lista de elementos de la página actual
     * @param paginaActual Número de página actual (0-indexed)
     * @param tamanoPagina Tamaño de la página
     * @param totalElementos Total de elementos en todas las páginas
     * @param <T> Tipo de entidad
     * @return PaginacionDTO con la información de la página
     */
    public static <T> PaginacionDTO<T> crear(List<T> contenido, int paginaActual, 
                                             int tamanoPagina, long totalElementos) {
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanoPagina);
        return new PaginacionDTO<>(contenido, paginaActual, tamanoPagina, 
                                   totalElementos, totalPaginas);
    }
    
    /**
     * Agrega atributos de paginación al modelo de Thymeleaf
     * Útil para mantener consistencia en las vistas
     * 
     * @param model Modelo de Spring MVC
     * @param paginacion DTO con información de paginación
     * @param nombreAtributo Nombre del atributo para el contenido (ej: "usuarios", "clientes")
     */
    public static <T> void agregarAtributos(Model model, PaginacionDTO<T> paginacion, 
                                           String nombreAtributo) {
        model.addAttribute(nombreAtributo, paginacion.getContenido());
        model.addAttribute("currentPage", paginacion.getPaginaActual());
        model.addAttribute("totalPages", paginacion.getTotalPaginas());
        model.addAttribute("totalItems", paginacion.getTotalElementos());
        model.addAttribute("pageSize", paginacion.getTamanoPagina());
    }
    
    /**
     * Agrega atributos de paginación con información de ordenamiento
     * 
     * @param model Modelo de Spring MVC
     * @param paginacion DTO con información de paginación
     * @param nombreAtributo Nombre del atributo para el contenido
     * @param sortBy Campo de ordenamiento
     * @param sortDir Dirección de ordenamiento (asc/desc)
     */
    public static <T> void agregarAtributosConOrdenamiento(Model model, 
                                                          PaginacionDTO<T> paginacion,
                                                          String nombreAtributo,
                                                          String sortBy, 
                                                          String sortDir) {
        agregarAtributos(model, paginacion, nombreAtributo);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
    }
}
