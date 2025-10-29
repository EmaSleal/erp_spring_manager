package api.astro.whats_orders_manager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.astro.whats_orders_manager.models.PlantillaWhatsApp;
import api.astro.whats_orders_manager.models.PlantillaWhatsApp.CategoriaPlantilla;
import api.astro.whats_orders_manager.models.PlantillaWhatsApp.EstadoMeta;

import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad PlantillaWhatsApp.
 * Proporciona métodos para gestionar plantillas de WhatsApp en la base de datos.
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - 26 octubre 2025
 */
@Repository
public interface PlantillaWhatsAppRepository extends JpaRepository<PlantillaWhatsApp, Integer> {
    
    /**
     * Busca una plantilla por su nombre interno
     * 
     * @param nombre Nombre de la plantilla (ej: "factura_generada")
     * @return Optional con la plantilla si existe
     */
    Optional<PlantillaWhatsApp> findByNombre(String nombre);
    
    /**
     * Busca una plantilla por su código en Meta
     * 
     * @param codigoMeta Código de la plantilla en Meta Business Manager
     * @return Optional con la plantilla si existe
     */
    Optional<PlantillaWhatsApp> findByCodigoMeta(String codigoMeta);
    
    /**
     * Busca una plantilla por su Template ID de Meta
     * 
     * @param templateId ID de la plantilla en Meta
     * @return Optional con la plantilla si existe
     */
    Optional<PlantillaWhatsApp> findByTemplateId(String templateId);
    
    /**
     * Obtiene todas las plantillas activas ordenadas por nombre
     * 
     * @return Lista de plantillas activas
     */
    List<PlantillaWhatsApp> findByActivoTrueOrderByNombre();
    
    /**
     * Busca plantillas por estado de aprobación en Meta
     * 
     * @param estadoMeta Estado de la plantilla (PENDING, APPROVED, REJECTED)
     * @return Lista de plantillas con ese estado
     */
    List<PlantillaWhatsApp> findByEstadoMeta(EstadoMeta estadoMeta);
    
    /**
     * Obtiene plantillas aprobadas y activas
     * Estas son las plantillas listas para usar
     * 
     * @param estadoMeta Estado de aprobación
     * @return Lista de plantillas aprobadas y activas
     */
    List<PlantillaWhatsApp> findByEstadoMetaAndActivoTrue(EstadoMeta estadoMeta);
    
    /**
     * Verifica si existe una plantilla con ese nombre
     * 
     * @param nombre Nombre de la plantilla
     * @return true si existe, false si no
     */
    boolean existsByNombre(String nombre);
    
    /**
     * Busca plantillas por categoría
     * 
     * @param categoria Categoría de la plantilla (UTILITY, MARKETING, AUTHENTICATION)
     * @return Lista de plantillas de esa categoría
     */
    List<PlantillaWhatsApp> findByCategoria(CategoriaPlantilla categoria);
    
    /**
     * Obtiene plantillas activas de una categoría específica
     * 
     * @param categoria Categoría de la plantilla
     * @return Lista de plantillas activas de esa categoría
     */
    List<PlantillaWhatsApp> findByCategoriaAndActivoTrue(CategoriaPlantilla categoria);
    
    /**
     * Cuenta plantillas por estado
     * Útil para estadísticas
     * 
     * @param estadoMeta Estado de aprobación
     * @return Cantidad de plantillas en ese estado
     */
    Long countByEstadoMeta(EstadoMeta estadoMeta);
    
    /**
     * Obtiene todas las plantillas ordenadas por fecha de creación descendente
     * 
     * @return Lista de plantillas ordenadas
     */
    List<PlantillaWhatsApp> findAllByOrderByCreateDateDesc();
}
