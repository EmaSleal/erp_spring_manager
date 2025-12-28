package api.astro.whats_orders_manager.modules.notificacion.service;

import api.astro.whats_orders_manager.modules.notificacion.model.PlantillaNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * PLANTILLA NOTIFICACIÓN SERVICE
 * WhatsApp Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Servicio para gestionar plantillas de notificaciones con versionado.
 * 
 * Funcionalidades:
 * - Crear y gestionar plantillas reutilizables
 * - Sistema de versionado de plantillas
 * - Procesamiento de variables dinámicas ({{variable}})
 * - Plantillas predeterminadas y del sistema
 * - Generación de contenido a partir de plantillas
 * ============================================================================
 */
public interface PlantillaNotificacionService {

    // ==================== CRUD ====================

    /**
     * Obtiene una plantilla por ID
     * 
     * @param idPlantilla ID de la plantilla
     * @return Optional con la plantilla
     */
    Optional<PlantillaNotificacion> findById(Integer idPlantilla);

    /**
     * Obtiene una plantilla por código único
     * 
     * @param codigo Código identificador de la plantilla
     * @return Optional con la plantilla
     */
    Optional<PlantillaNotificacion> findByCodigo(String codigo);

    /**
     * Obtiene todas las plantillas (paginadas)
     * 
     * @param pageable Paginación
     * @return Page con plantillas
     */
    Page<PlantillaNotificacion> findAll(Pageable pageable);

    /**
     * Obtiene plantillas activas (paginadas)
     * 
     * @param pageable Paginación
     * @return Page con plantillas activas
     */
    Page<PlantillaNotificacion> findActivas(Pageable pageable);

    /**
     * Crea una nueva plantilla
     * 
     * @param plantilla Plantilla a crear
     * @return Plantilla creada
     */
    PlantillaNotificacion crear(PlantillaNotificacion plantilla);

    /**
     * Actualiza una plantilla existente
     * 
     * @param idPlantilla ID de la plantilla
     * @param plantilla Datos actualizados
     * @return Plantilla actualizada
     */
    PlantillaNotificacion actualizar(Integer idPlantilla, PlantillaNotificacion plantilla);

    /**
     * Elimina una plantilla (solo si no es del sistema)
     * 
     * @param idPlantilla ID de la plantilla
     * @return true si se eliminó correctamente
     */
    boolean eliminar(Integer idPlantilla);

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================

    /**
     * Obtiene plantillas por tipo (paginadas)
     * 
     * @param tipo Tipo de notificación
     * @param pageable Paginación
     * @return Page con plantillas del tipo
     */
    Page<PlantillaNotificacion> findByTipo(TipoNotificacion tipo, Pageable pageable);

    /**
     * Obtiene plantillas por canal (paginadas)
     * 
     * @param canal Canal de notificación
     * @param pageable Paginación
     * @return Page con plantillas del canal
     */
    Page<PlantillaNotificacion> findByCanal(CanalNotificacion canal, Pageable pageable);

    /**
     * Obtiene plantillas por tipo Y canal (paginadas)
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @param pageable Paginación
     * @return Page con plantillas filtradas
     */
    Page<PlantillaNotificacion> findByTipoAndCanal(TipoNotificacion tipo, CanalNotificacion canal, Pageable pageable);

    /**
     * Obtiene plantillas del sistema
     * 
     * @return Lista de plantillas del sistema
     */
    List<PlantillaNotificacion> findPlantillasSistema();

    /**
     * Obtiene plantillas creadas por usuarios
     * 
     * @return Lista de plantillas personalizadas
     */
    List<PlantillaNotificacion> findPlantillasPersonalizadas();

    // ==================== VERSIONADO ====================

    /**
     * Obtiene todas las versiones de una plantilla
     * 
     * @param nombre Nombre de la plantilla
     * @return Lista de versiones ordenadas DESC
     */
    List<PlantillaNotificacion> findVersiones(String nombre);

    /**
     * Obtiene la última versión de una plantilla
     * 
     * @param nombre Nombre de la plantilla
     * @return Optional con la última versión
     */
    Optional<PlantillaNotificacion> findUltimaVersion(String nombre);

    /**
     * Obtiene una versión específica de una plantilla
     * 
     * @param nombre Nombre de la plantilla
     * @param version Número de versión
     * @return Optional con la versión solicitada
     */
    Optional<PlantillaNotificacion> findVersion(String nombre, Integer version);

    /**
     * Crea una nueva versión de una plantilla existente
     * 
     * @param idPlantillaBase ID de la plantilla base
     * @return Nueva versión de la plantilla
     */
    PlantillaNotificacion crearNuevaVersion(Integer idPlantillaBase);

    // ==================== PLANTILLAS PREDETERMINADAS ====================

    /**
     * Obtiene la plantilla predeterminada para un tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Optional con la plantilla predeterminada
     */
    Optional<PlantillaNotificacion> obtenerPlantillaPredeterminada(TipoNotificacion tipo, CanalNotificacion canal);

    /**
     * Establece una plantilla como predeterminada para su tipo y canal
     * 
     * @param idPlantilla ID de la plantilla
     * @return true si se estableció correctamente
     */
    boolean establecerComoPredeterminada(Integer idPlantilla);

    /**
     * Verifica si existe una plantilla predeterminada para tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return true si existe
     */
    boolean existePlantillaPredeterminada(TipoNotificacion tipo, CanalNotificacion canal);

    // ==================== ACTIVACIÓN/DESACTIVACIÓN ====================

    /**
     * Activa una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return true si se activó correctamente
     */
    boolean activar(Integer idPlantilla);

    /**
     * Desactiva una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return true si se desactivó correctamente
     */
    boolean desactivar(Integer idPlantilla);

    // ==================== PROCESAMIENTO DE CONTENIDO ====================

    /**
     * Procesa el contenido de una plantilla reemplazando variables
     * 
     * @param idPlantilla ID de la plantilla
     * @param variables Map con variables a reemplazar
     * @return Contenido procesado
     */
    String procesarContenido(Integer idPlantilla, Map<String, Object> variables);

    /**
     * Procesa el asunto de una plantilla reemplazando variables
     * 
     * @param idPlantilla ID de la plantilla
     * @param variables Map con variables a reemplazar
     * @return Asunto procesado
     */
    String procesarAsunto(Integer idPlantilla, Map<String, Object> variables);

    /**
     * Procesa una plantilla completa (asunto + contenido)
     * 
     * @param idPlantilla ID de la plantilla
     * @param variables Map con variables a reemplazar
     * @return Map con "asunto" y "contenido" procesados
     */
    Map<String, String> procesarPlantilla(Integer idPlantilla, Map<String, Object> variables);

    /**
     * Obtiene las variables disponibles en una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return Lista de nombres de variables (sin {{}})
     */
    List<String> obtenerVariablesDisponibles(Integer idPlantilla);

    /**
     * Obtiene las variables necesarias que faltan en el Map proporcionado
     * 
     * @param idPlantilla ID de la plantilla
     * @param variables Map con variables proporcionadas
     * @return Lista de variables faltantes
     */
    List<String> obtenerVariablesNecesarias(Integer idPlantilla, Map<String, Object> variables);

    /**
     * Valida que una plantilla tenga todas las variables necesarias
     * 
     * @param idPlantilla ID de la plantilla
     * @param variables Map con variables a validar
     * @return true si todas las variables están presentes
     */
    boolean validarVariables(Integer idPlantilla, Map<String, Object> variables);

    // ==================== DATOS DE EJEMPLO ====================

    /**
     * Obtiene los datos de ejemplo de una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return JSON String con datos de ejemplo
     */
    String obtenerDatosEjemplo(Integer idPlantilla);

    /**
     * Actualiza los datos de ejemplo de una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @param datosEjemplo JSON String con datos de ejemplo
     * @return true si se actualizó correctamente
     */
    boolean actualizarDatosEjemplo(Integer idPlantilla, String datosEjemplo);

    /**
     * Genera una vista previa de la plantilla con datos de ejemplo
     * 
     * @param idPlantilla ID de la plantilla
     * @return Map con vista previa de asunto y contenido
     */
    Map<String, String> generarVistaPrevia(Integer idPlantilla);

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
     * Obtiene estadísticas de plantillas por tipo
     * 
     * @return Map con tipo y cantidad
     */
    Map<TipoNotificacion, Long> obtenerEstadisticasPorTipo();

    /**
     * Obtiene estadísticas de plantillas por canal
     * 
     * @return Map con canal y cantidad
     */
    Map<CanalNotificacion, Long> obtenerEstadisticasPorCanal();

    /**
     * Cuenta plantillas activas
     * 
     * @return Cantidad de plantillas activas
     */
    long countActivas();

    /**
     * Cuenta plantillas del sistema
     * 
     * @return Cantidad de plantillas del sistema
     */
    long countPlantillasSistema();
}
