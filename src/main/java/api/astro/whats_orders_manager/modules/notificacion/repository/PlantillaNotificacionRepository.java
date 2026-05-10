package api.astro.whats_orders_manager.modules.notificacion.repository;

import api.astro.whats_orders_manager.modules.notificacion.model.PlantillaNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * PLANTILLA NOTIFICACIÓN REPOSITORY
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Repositorio para gestionar las plantillas de notificaciones.
 * 
 * Funcionalidades:
 * - Consultar plantillas por tipo y canal
 * - Obtener plantillas activas y predeterminadas
 * - Gestionar versionado de plantillas
 * - Buscar plantillas del sistema
 * ============================================================================
 */
@Repository
public interface PlantillaNotificacionRepository extends JpaRepository<PlantillaNotificacion, Integer> {

    // ==================== BÚSQUEDAS POR CÓDIGO Y NOMBRE ====================

    /**
     * Busca una plantilla por su código único
     * 
     * @param codigo Código único de la plantilla
     * @return Optional con la plantilla si existe
     */
    Optional<PlantillaNotificacion> findByCodigo(String codigo);

    /**
     * Busca plantillas por nombre
     * 
     * @param nombre Nombre de la plantilla
     * @return Lista de plantillas con ese nombre (diferentes versiones)
     */
    List<PlantillaNotificacion> findByNombreOrderByVersionDesc(String nombre);

    /**
     * Busca plantillas por nombre parcial (búsqueda con LIKE)
     * 
     * @param nombre Nombre parcial
     * @param pageable Paginación
     * @return Page con plantillas que coinciden
     */
    @Query("SELECT p FROM PlantillaNotificacion p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "ORDER BY p.nombre, p.version DESC")
    Page<PlantillaNotificacion> buscarPorNombre(@Param("nombre") String nombre, Pageable pageable);

    // ==================== BÚSQUEDAS POR TIPO Y CANAL ====================

    /**
     * Busca todas las plantillas para un tipo específico
     * 
     * @param tipo Tipo de notificación
     * @return Lista de plantillas del tipo especificado
     */
    List<PlantillaNotificacion> findByTipoOrderByNombreAsc(TipoNotificacion tipo);

    /**
     * Busca todas las plantillas para un canal específico
     * 
     * @param canal Canal de notificación
     * @return Lista de plantillas del canal especificado
     */
    List<PlantillaNotificacion> findByCanalOrderByNombreAsc(CanalNotificacion canal);

    /**
     * Busca plantillas por tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Lista de plantillas que coinciden
     */
    List<PlantillaNotificacion> findByTipoAndCanalOrderByNombreAsc(
            TipoNotificacion tipo,
            CanalNotificacion canal
    );

    /**
     * Busca plantillas activas por tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Lista de plantillas activas
     */
    List<PlantillaNotificacion> findByTipoAndCanalAndActivaTrueOrderByNombreAsc(
            TipoNotificacion tipo,
            CanalNotificacion canal
    );

    // ==================== BÚSQUEDAS DE PLANTILLAS ACTIVAS ====================

    /**
     * Busca todas las plantillas activas
     * 
     * @param pageable Paginación
     * @return Page con plantillas activas
     */
    Page<PlantillaNotificacion> findByActivaTrueOrderByNombreAsc(Pageable pageable);

    /**
     * Busca todas las plantillas inactivas
     * 
     * @param pageable Paginación
     * @return Page con plantillas inactivas
     */
    Page<PlantillaNotificacion> findByActivaFalseOrderByNombreAsc(Pageable pageable);

    // ==================== BÚSQUEDAS DE PLANTILLAS PREDETERMINADAS ====================

    /**
     * Busca la plantilla predeterminada para un tipo y canal
     * Solo debe existir una plantilla predeterminada por tipo/canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Optional con la plantilla predeterminada si existe
     */
    @Query("SELECT p FROM PlantillaNotificacion p WHERE p.tipo = :tipo " +
           "AND p.canal = :canal AND p.predeterminada = true AND p.activa = true")
    Optional<PlantillaNotificacion> findPlantillaPredeterminada(
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal
    );

    /**
     * Busca todas las plantillas predeterminadas activas
     * 
     * @return Lista de plantillas predeterminadas
     */
    List<PlantillaNotificacion> findByPredeterminadaTrueAndActivaTrueOrderByTipoAsc();

    /**
     * Verifica si existe una plantilla predeterminada para tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return true si existe plantilla predeterminada
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM PlantillaNotificacion p WHERE p.tipo = :tipo " +
           "AND p.canal = :canal AND p.predeterminada = true AND p.activa = true")
    boolean existePlantillaPredeterminada(
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal
    );

    // ==================== BÚSQUEDAS POR VERSIÓN ====================

    /**
     * Busca la versión más reciente de una plantilla por nombre
     * 
     * @param nombre Nombre de la plantilla
     * @return Optional con la versión más reciente
     */
    @Query("SELECT p FROM PlantillaNotificacion p WHERE p.nombre = :nombre " +
           "ORDER BY p.version DESC LIMIT 1")
    Optional<PlantillaNotificacion> findUltimaVersion(@Param("nombre") String nombre);

    /**
     * Busca una versión específica de una plantilla
     * 
     * @param nombre Nombre de la plantilla
     * @param version Número de versión
     * @return Optional con la plantilla si existe
     */
    Optional<PlantillaNotificacion> findByNombreAndVersion(String nombre, Integer version);

    // ==================== BÚSQUEDAS DE PLANTILLAS DEL SISTEMA ====================

    /**
     * Busca todas las plantillas del sistema (no editables)
     * 
     * @return Lista de plantillas del sistema
     */
    List<PlantillaNotificacion> findByPlantillaSistemaTrueOrderByNombreAsc();

    /**
     * Busca todas las plantillas personalizadas (editables)
     * 
     * @return Lista de plantillas personalizadas
     */
    List<PlantillaNotificacion> findByPlantillaSistemaFalseOrderByNombreAsc();

    /**
     * Busca plantillas del sistema por tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Lista de plantillas del sistema
     */
    List<PlantillaNotificacion> findByTipoAndCanalAndPlantillaSistemaTrue(
            TipoNotificacion tipo,
            CanalNotificacion canal
    );

    // ==================== OPERACIONES DE ACTUALIZACIÓN ====================

    /**
     * Activa una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PlantillaNotificacion p SET p.activa = true WHERE p.idPlantilla = :idPlantilla")
    int activarPlantilla(@Param("idPlantilla") Integer idPlantilla);

    /**
     * Desactiva una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PlantillaNotificacion p SET p.activa = false WHERE p.idPlantilla = :idPlantilla")
    int desactivarPlantilla(@Param("idPlantilla") Integer idPlantilla);

    /**
     * Establece una plantilla como predeterminada
     * Primero quita el estado predeterminado de otras plantillas del mismo tipo/canal
     * 
     * @param idPlantilla ID de la plantilla
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PlantillaNotificacion p SET p.predeterminada = false " +
           "WHERE p.tipo = :tipo AND p.canal = :canal AND p.idPlantilla != :idPlantilla")
    int quitarPredeterminadasPreviasParaTipoCanal(
            @Param("idPlantilla") Integer idPlantilla,
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal
    );

    /**
     * Establece una plantilla como predeterminada
     * 
     * @param idPlantilla ID de la plantilla
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PlantillaNotificacion p SET p.predeterminada = true WHERE p.idPlantilla = :idPlantilla")
    int establecerComoPredeterminada(@Param("idPlantilla") Integer idPlantilla);

    /**
     * Quita el estado predeterminado de una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PlantillaNotificacion p SET p.predeterminada = false WHERE p.idPlantilla = :idPlantilla")
    int quitarPredeterminada(@Param("idPlantilla") Integer idPlantilla);

    // ==================== ESTADÍSTICAS ====================

    /**
     * Cuenta plantillas por tipo
     * 
     * @param tipo Tipo de notificación
     * @return Cantidad de plantillas
     */
    long countByTipo(TipoNotificacion tipo);

    /**
     * Cuenta plantillas por canal
     * 
     * @param canal Canal de notificación
     * @return Cantidad de plantillas
     */
    long countByCanal(CanalNotificacion canal);

    /**
     * Cuenta plantillas activas
     * 
     * @return Cantidad de plantillas activas
     */
    long countByActivaTrue();

    /**
     * Cuenta plantillas del sistema
     * 
     * @return Cantidad de plantillas del sistema
     */
    long countByPlantillaSistemaTrue();

    /**
     * Obtiene estadísticas de plantillas por tipo
     * 
     * @return Lista de objetos con tipo y cantidad
     */
    @Query("SELECT p.tipo as tipo, COUNT(p) as cantidad FROM PlantillaNotificacion p " +
           "GROUP BY p.tipo ORDER BY cantidad DESC")
    List<Object[]> obtenerEstadisticasPorTipo();

    /**
     * Obtiene estadísticas de plantillas por canal
     * 
     * @return Lista de objetos con canal y cantidad
     */
    @Query("SELECT p.canal as canal, COUNT(p) as cantidad FROM PlantillaNotificacion p " +
           "GROUP BY p.canal ORDER BY cantidad DESC")
    List<Object[]> obtenerEstadisticasPorCanal();

    // ==================== VERIFICACIÓN DE EXISTENCIA ====================

    /**
     * Verifica si existe una plantilla con el código especificado
     * 
     * @param codigo Código de la plantilla
     * @return true si existe
     */
    boolean existsByCodigo(String codigo);

    /**
     * Verifica si existe una plantilla con nombre y versión
     * 
     * @param nombre Nombre de la plantilla
     * @param version Versión de la plantilla
     * @return true si existe
     */
    boolean existsByNombreAndVersion(String nombre, Integer version);
}
