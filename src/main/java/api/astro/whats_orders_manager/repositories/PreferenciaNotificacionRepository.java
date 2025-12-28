package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.PreferenciaNotificacion;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.models.enums.CanalNotificacion;
import api.astro.whats_orders_manager.models.enums.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * PREFERENCIA NOTIFICACIÓN REPOSITORY
 * WhatsApp Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Repositorio para gestionar las preferencias de notificaciones de usuarios.
 * 
 * Funcionalidades:
 * - Consultar preferencias por usuario
 * - Filtrar por tipo y canal
 * - Verificar si un usuario acepta notificaciones
 * - Gestionar activación/desactivación de notificaciones
 * ============================================================================
 */
@Repository
public interface PreferenciaNotificacionRepository extends JpaRepository<PreferenciaNotificacion, Integer> {

    // ==================== BÚSQUEDAS POR USUARIO ====================

    /**
     * Busca todas las preferencias de un usuario
     * 
     * @param usuario Usuario propietario
     * @return Lista de preferencias
     */
    List<PreferenciaNotificacion> findByUsuario(Usuario usuario);

    /**
     * Busca preferencias de un usuario por ID
     * 
     * @param idUsuario ID del usuario
     * @return Lista de preferencias
     */
    @Query("SELECT p FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario")
    List<PreferenciaNotificacion> findByUsuarioId(@Param("idUsuario") Integer idUsuario);

    /**
     * Busca preferencias activas de un usuario
     * 
     * @param usuario Usuario propietario
     * @return Lista de preferencias activas
     */
    List<PreferenciaNotificacion> findByUsuarioAndActivaTrue(Usuario usuario);

    /**
     * Busca preferencias activas por ID de usuario
     * 
     * @param idUsuario ID del usuario
     * @return Lista de preferencias activas
     */
    @Query("SELECT p FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario AND p.activa = true")
    List<PreferenciaNotificacion> findActivasByUsuarioId(@Param("idUsuario") Integer idUsuario);

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================

    /**
     * Busca una preferencia específica para tipo y canal
     * 
     * @param usuario Usuario propietario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Optional con la preferencia si existe
     */
    Optional<PreferenciaNotificacion> findByUsuarioAndTipoNotificacionAndCanal(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal
    );

    /**
     * Busca una preferencia específica por ID de usuario
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Optional con la preferencia si existe
     */
    @Query("SELECT p FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario " +
           "AND p.tipoNotificacion = :tipo AND p.canal = :canal")
    Optional<PreferenciaNotificacion> findByUsuarioIdAndTipoAndCanal(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal
    );

    /**
     * Busca preferencias de un usuario para un tipo específico (todos los canales)
     * 
     * @param usuario Usuario propietario
     * @param tipo Tipo de notificación
     * @return Lista de preferencias para ese tipo
     */
    List<PreferenciaNotificacion> findByUsuarioAndTipoNotificacion(Usuario usuario, TipoNotificacion tipo);

    /**
     * Busca preferencias de un usuario para un canal específico (todos los tipos)
     * 
     * @param usuario Usuario propietario
     * @param canal Canal de notificación
     * @return Lista de preferencias para ese canal
     */
    List<PreferenciaNotificacion> findByUsuarioAndCanal(Usuario usuario, CanalNotificacion canal);

    // ==================== BÚSQUEDAS GLOBALES ====================

    /**
     * Busca la preferencia global de un usuario (tipo y canal NULL)
     * Esta preferencia aplica a todas las notificaciones
     * 
     * @param usuario Usuario propietario
     * @return Optional con la preferencia global si existe
     */
    @Query("SELECT p FROM PreferenciaNotificacion p WHERE p.usuario = :usuario " +
           "AND p.tipoNotificacion IS NULL AND p.canal IS NULL")
    Optional<PreferenciaNotificacion> findPreferenciaGlobal(@Param("usuario") Usuario usuario);

    /**
     * Busca la preferencia global por ID de usuario
     * 
     * @param idUsuario ID del usuario
     * @return Optional con la preferencia global si existe
     */
    @Query("SELECT p FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario " +
           "AND p.tipoNotificacion IS NULL AND p.canal IS NULL")
    Optional<PreferenciaNotificacion> findPreferenciaGlobalByUsuarioId(@Param("idUsuario") Integer idUsuario);

    /**
     * Verifica si un usuario tiene notificaciones desactivadas globalmente
     * 
     * @param usuario Usuario a verificar
     * @return true si las notificaciones están desactivadas globalmente
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM PreferenciaNotificacion p WHERE p.usuario = :usuario " +
           "AND p.notificacionesDesactivadasGlobal = true")
    boolean tieneNotificacionesDesactivadasGlobal(@Param("usuario") Usuario usuario);

    /**
     * Verifica si un usuario tiene notificaciones desactivadas globalmente por ID
     * 
     * @param idUsuario ID del usuario
     * @return true si las notificaciones están desactivadas globalmente
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario " +
           "AND p.notificacionesDesactivadasGlobal = true")
    boolean tieneNotificacionesDesactivadasGlobalByUsuarioId(@Param("idUsuario") Integer idUsuario);

    // ==================== VERIFICACIÓN DE ACEPTACIÓN ====================

    /**
     * Verifica si un usuario acepta notificaciones de un tipo y canal específicos
     * Considera preferencias globales, por tipo, por canal y específicas
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return true si el usuario acepta este tipo de notificación
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario " +
           "AND p.activa = true " +
           "AND p.notificacionesDesactivadasGlobal = false " +
           "AND ((p.tipoNotificacion = :tipo AND p.canal = :canal) " +
           "     OR (p.tipoNotificacion = :tipo AND p.canal IS NULL) " +
           "     OR (p.tipoNotificacion IS NULL AND p.canal = :canal) " +
           "     OR (p.tipoNotificacion IS NULL AND p.canal IS NULL))")
    boolean usuarioAceptaNotificacion(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal
    );

    /**
     * Busca todas las preferencias aplicables para un tipo y canal
     * (específicas, por tipo, por canal y globales)
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Lista de preferencias aplicables (ordenadas por especificidad)
     */
    @Query("SELECT p FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario " +
           "AND ((p.tipoNotificacion = :tipo AND p.canal = :canal) " +
           "     OR (p.tipoNotificacion = :tipo AND p.canal IS NULL) " +
           "     OR (p.tipoNotificacion IS NULL AND p.canal = :canal) " +
           "     OR (p.tipoNotificacion IS NULL AND p.canal IS NULL)) " +
           "ORDER BY " +
           "  CASE " +
           "    WHEN p.tipoNotificacion IS NOT NULL AND p.canal IS NOT NULL THEN 1 " +
           "    WHEN p.tipoNotificacion IS NOT NULL THEN 2 " +
           "    WHEN p.canal IS NOT NULL THEN 3 " +
           "    ELSE 4 " +
           "  END")
    List<PreferenciaNotificacion> findPreferenciasAplicables(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal
    );

    // ==================== OPERACIONES DE ACTUALIZACIÓN ====================

    /**
     * Activa una preferencia específica
     * 
     * @param idPreferencia ID de la preferencia
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PreferenciaNotificacion p SET p.activa = true WHERE p.idPreferencia = :idPreferencia")
    int activarPreferencia(@Param("idPreferencia") Integer idPreferencia);

    /**
     * Desactiva una preferencia específica
     * 
     * @param idPreferencia ID de la preferencia
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PreferenciaNotificacion p SET p.activa = false WHERE p.idPreferencia = :idPreferencia")
    int desactivarPreferencia(@Param("idPreferencia") Integer idPreferencia);

    /**
     * Desactiva todas las notificaciones de un usuario (global)
     * 
     * @param idUsuario ID del usuario
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PreferenciaNotificacion p SET p.notificacionesDesactivadasGlobal = true " +
           "WHERE p.usuario.idUsuario = :idUsuario")
    int desactivarTodasLasNotificaciones(@Param("idUsuario") Integer idUsuario);

    /**
     * Activa todas las notificaciones de un usuario (global)
     * 
     * @param idUsuario ID del usuario
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE PreferenciaNotificacion p SET p.notificacionesDesactivadasGlobal = false " +
           "WHERE p.usuario.idUsuario = :idUsuario")
    int activarTodasLasNotificaciones(@Param("idUsuario") Integer idUsuario);

    // ==================== ESTADÍSTICAS ====================

    /**
     * Cuenta cuántos usuarios tienen notificaciones activadas
     * 
     * @return Cantidad de usuarios con notificaciones activas
     */
    @Query("SELECT COUNT(DISTINCT p.usuario) FROM PreferenciaNotificacion p " +
           "WHERE p.activa = true AND p.notificacionesDesactivadasGlobal = false")
    long contarUsuariosConNotificacionesActivas();

    /**
     * Cuenta cuántos usuarios tienen notificaciones desactivadas globalmente
     * 
     * @return Cantidad de usuarios con notificaciones desactivadas
     */
    @Query("SELECT COUNT(DISTINCT p.usuario) FROM PreferenciaNotificacion p " +
           "WHERE p.notificacionesDesactivadasGlobal = true")
    long contarUsuariosConNotificacionesDesactivadas();

    /**
     * Obtiene estadísticas de preferencias por tipo
     * 
     * @return Lista de objetos con tipo y cantidad de usuarios que lo aceptan
     */
    @Query("SELECT p.tipoNotificacion as tipo, COUNT(DISTINCT p.usuario) as cantidad " +
           "FROM PreferenciaNotificacion p WHERE p.activa = true " +
           "AND p.tipoNotificacion IS NOT NULL GROUP BY p.tipoNotificacion")
    List<Object[]> obtenerEstadisticasPorTipo();

    /**
     * Obtiene estadísticas de preferencias por canal
     * 
     * @return Lista de objetos con canal y cantidad de usuarios que lo aceptan
     */
    @Query("SELECT p.canal as canal, COUNT(DISTINCT p.usuario) as cantidad " +
           "FROM PreferenciaNotificacion p WHERE p.activa = true " +
           "AND p.canal IS NOT NULL GROUP BY p.canal")
    List<Object[]> obtenerEstadisticasPorCanal();

    // ==================== VERIFICACIÓN DE EXISTENCIA ====================

    /**
     * Verifica si existe una preferencia para usuario, tipo y canal
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return true si existe la preferencia
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM PreferenciaNotificacion p WHERE p.usuario.idUsuario = :idUsuario " +
           "AND p.tipoNotificacion = :tipo AND p.canal = :canal")
    boolean existePreferencia(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal
    );
}
