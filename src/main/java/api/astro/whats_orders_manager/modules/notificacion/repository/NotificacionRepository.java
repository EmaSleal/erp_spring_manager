package api.astro.whats_orders_manager.modules.notificacion.repository;

import api.astro.whats_orders_manager.modules.notificacion.model.Notificacion;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * NOTIFICACIÓN REPOSITORY
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Repositorio para acceder al historial de notificaciones enviadas.
 * 
 * Funcionalidades:
 * - Consultar notificaciones por usuario
 * - Filtrar por estado (leídas/no leídas)
 * - Filtrar por tipo y canal
 * - Marcar notificaciones como leídas
 * - Obtener estadísticas de notificaciones
 * ============================================================================
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    // ==================== BÚSQUEDAS POR USUARIO ====================

    /**
     * Busca todas las notificaciones de un usuario
     * Ordenadas por fecha de envío descendente (más recientes primero)
     * 
     * @param usuario Usuario destinatario
     * @param pageable Paginación
     * @return Page con notificaciones del usuario
     */
    Page<Notificacion> findByUsuarioOrderByFechaEnvioDesc(Usuario usuario, Pageable pageable);

    /**
     * Busca notificaciones de un usuario por ID
     * 
     * @param idUsuario ID del usuario
     * @param pageable Paginación
     * @return Page con notificaciones
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario ORDER BY n.fechaEnvio DESC")
    Page<Notificacion> findByUsuarioId(@Param("idUsuario") Integer idUsuario, Pageable pageable);

    /**
     * Busca notificaciones NO LEÍDAS de un usuario
     * 
     * @param usuario Usuario destinatario
     * @return Lista de notificaciones no leídas
     */
    List<Notificacion> findByUsuarioAndLeidaFalseOrderByFechaEnvioDesc(Usuario usuario);

    /**
     * Busca notificaciones NO LEÍDAS por ID de usuario
     * 
     * @param idUsuario ID del usuario
     * @return Lista de notificaciones no leídas
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario AND n.leida = false ORDER BY n.fechaEnvio DESC")
    List<Notificacion> findNoLeidasByUsuarioId(@Param("idUsuario") Integer idUsuario);

    /**
     * Cuenta notificaciones NO LEÍDAS de un usuario
     * 
     * @param usuario Usuario destinatario
     * @return Cantidad de notificaciones no leídas
     */
    long countByUsuarioAndLeidaFalse(Usuario usuario);

    /**
     * Cuenta notificaciones NO LEÍDAS por ID de usuario
     * 
     * @param idUsuario ID del usuario
     * @return Cantidad de notificaciones no leídas
     */
    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario AND n.leida = false")
    long countNoLeidasByUsuarioId(@Param("idUsuario") Integer idUsuario);

    // ==================== MÉTODOS ADICIONALES PARA API REST ====================

    /**
     * Busca notificaciones de un usuario filtradas por estado de lectura
     * 
     * @param idUsuario ID del usuario
     * @param leida Estado de lectura (true/false)
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario AND n.leida = :leida ORDER BY n.fechaEnvio DESC")
    Page<Notificacion> findByUsuarioIdAndLeida(
            @Param("idUsuario") Integer idUsuario,
            @Param("leida") Boolean leida,
            Pageable pageable
    );

    /**
     * Busca notificaciones de un usuario por tipo
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario AND n.tipo = :tipo ORDER BY n.fechaEnvio DESC")
    Page<Notificacion> findByUsuarioIdAndTipo(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoNotificacion tipo,
            Pageable pageable
    );

    /**
     * Busca notificaciones de un usuario por tipo y estado de lectura
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param leida Estado de lectura
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario AND n.tipo = :tipo AND n.leida = :leida ORDER BY n.fechaEnvio DESC")
    Page<Notificacion> findByUsuarioIdAndTipoAndLeida(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoNotificacion tipo,
            @Param("leida") Boolean leida,
            Pageable pageable
    );

    // ==================== BÚSQUEDAS POR TIPO Y CANAL ====================

    /**
     * Busca notificaciones por tipo
     * 
     * @param tipo Tipo de notificación
     * @param pageable Paginación
     * @return Page con notificaciones del tipo especificado
     */
    Page<Notificacion> findByTipoOrderByFechaEnvioDesc(TipoNotificacion tipo, Pageable pageable);

    /**
     * Busca notificaciones por canal
     * 
     * @param canal Canal de notificación
     * @param pageable Paginación
     * @return Page con notificaciones del canal especificado
     */
    Page<Notificacion> findByCanalOrderByFechaEnvioDesc(CanalNotificacion canal, Pageable pageable);

    /**
     * Busca notificaciones por tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    Page<Notificacion> findByTipoAndCanalOrderByFechaEnvioDesc(
            TipoNotificacion tipo, 
            CanalNotificacion canal, 
            Pageable pageable
    );

    /**
     * Busca notificaciones de un usuario por tipo y canal
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario " +
           "AND n.tipo = :tipo AND n.canal = :canal ORDER BY n.fechaEnvio DESC")
    Page<Notificacion> findByUsuarioIdAndTipoAndCanal(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoNotificacion tipo,
            @Param("canal") CanalNotificacion canal,
            Pageable pageable
    );

    // ==================== BÚSQUEDAS POR ESTADO ====================

    /**
     * Busca notificaciones enviadas exitosamente
     * 
     * @param pageable Paginación
     * @return Page con notificaciones enviadas
     */
    Page<Notificacion> findByEnviadaTrueOrderByFechaEnvioDesc(Pageable pageable);

    /**
     * Busca notificaciones con errores de envío
     * 
     * @param pageable Paginación
     * @return Page con notificaciones fallidas
     */
    Page<Notificacion> findByEnviadaFalseOrderByFechaEnvioDesc(Pageable pageable);

    /**
     * Busca notificaciones con errores para reintentar
     * (no enviadas y con menos de X intentos)
     * 
     * @param maxIntentos Máximo número de intentos permitidos
     * @return Lista de notificaciones para reintentar
     */
    @Query("SELECT n FROM Notificacion n WHERE n.enviada = false " +
           "AND n.intentosEnvio < :maxIntentos ORDER BY n.fechaEnvio ASC")
    List<Notificacion> findFallidasParaReintentar(@Param("maxIntentos") Integer maxIntentos);

    // ==================== BÚSQUEDAS POR RANGO DE FECHAS ====================

    /**
     * Busca notificaciones en un rango de fechas
     * 
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @param pageable Paginación
     * @return Page con notificaciones en el rango
     */
    @Query("SELECT n FROM Notificacion n WHERE n.fechaEnvio BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY n.fechaEnvio DESC")
    Page<Notificacion> findByRangoFechas(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    /**
     * Busca notificaciones de un usuario en un rango de fechas
     * 
     * @param idUsuario ID del usuario
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario " +
           "AND n.fechaEnvio BETWEEN :fechaInicio AND :fechaFin ORDER BY n.fechaEnvio DESC")
    Page<Notificacion> findByUsuarioIdAndRangoFechas(
            @Param("idUsuario") Integer idUsuario,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    // ==================== BÚSQUEDAS POR ENTIDADES RELACIONADAS ====================

    /**
     * Busca notificaciones relacionadas con una factura
     * 
     * @param idFactura ID de la factura
     * @return Lista de notificaciones relacionadas
     */
    List<Notificacion> findByIdFacturaRelacionadaOrderByFechaEnvioDesc(Integer idFactura);

    /**
     * Busca notificaciones relacionadas con un cliente
     * 
     * @param idCliente ID del cliente
     * @return Lista de notificaciones relacionadas
     */
    List<Notificacion> findByIdClienteRelacionadoOrderByFechaEnvioDesc(Integer idCliente);

    /**
     * Busca notificaciones relacionadas con un producto
     * 
     * @param idProducto ID del producto
     * @return Lista de notificaciones relacionadas
     */
    List<Notificacion> findByIdProductoRelacionadoOrderByFechaEnvioDesc(Integer idProducto);

    // ==================== OPERACIONES DE ACTUALIZACIÓN ====================

    /**
     * Marca una notificación como leída
     * 
     * @param idNotificacion ID de la notificación
     * @param fechaLectura Fecha y hora de lectura
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLectura = :fechaLectura " +
           "WHERE n.idNotificacion = :idNotificacion")
    int marcarComoLeida(
            @Param("idNotificacion") Integer idNotificacion,
            @Param("fechaLectura") LocalDateTime fechaLectura
    );

    /**
     * Marca todas las notificaciones de un usuario como leídas
     * 
     * @param idUsuario ID del usuario
     * @param fechaLectura Fecha y hora de lectura
     * @return Número de registros actualizados
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLectura = :fechaLectura " +
           "WHERE n.usuario.idUsuario = :idUsuario AND n.leida = false")
    int marcarTodasComoLeidas(
            @Param("idUsuario") Integer idUsuario,
            @Param("fechaLectura") LocalDateTime fechaLectura
    );

    // ==================== ESTADÍSTICAS ====================

    /**
     * Cuenta notificaciones por tipo
     * 
     * @param tipo Tipo de notificación
     * @return Cantidad de notificaciones
     */
    long countByTipo(TipoNotificacion tipo);

    /**
     * Cuenta notificaciones por canal
     * 
     * @param canal Canal de notificación
     * @return Cantidad de notificaciones
     */
    long countByCanal(CanalNotificacion canal);

    /**
     * Cuenta notificaciones enviadas exitosamente
     * 
     * @return Cantidad de notificaciones enviadas
     */
    long countByEnviadaTrue();

    /**
     * Cuenta notificaciones fallidas
     * 
     * @return Cantidad de notificaciones con error
     */
    long countByEnviadaFalse();

    /**
     * Obtiene estadísticas de notificaciones por tipo
     * 
     * @return Lista de objetos con tipo y cantidad
     */
    @Query("SELECT n.tipo as tipo, COUNT(n) as cantidad FROM Notificacion n " +
           "GROUP BY n.tipo ORDER BY cantidad DESC")
    List<Object[]> obtenerEstadisticasPorTipo();

    /**
     * Obtiene estadísticas de notificaciones por canal
     * 
     * @return Lista de objetos con canal y cantidad
     */
    @Query("SELECT n.canal as canal, COUNT(n) as cantidad FROM Notificacion n " +
           "GROUP BY n.canal ORDER BY cantidad DESC")
    List<Object[]> obtenerEstadisticasPorCanal();

    /**
     * Obtiene las últimas notificaciones de un usuario (usa Pageable para limitar resultados)
     * 
     * @param idUsuario ID del usuario
     * @param pageable Paginación (usar PageRequest.of(0, limite) para fijar el límite)
     * @return Lista de notificaciones recientes
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario ORDER BY n.fechaEnvio DESC")
    List<Notificacion> findUltimasNotificaciones(
            @Param("idUsuario") Integer idUsuario,
            Pageable pageable
    );

    // ==================== LIMPIEZA ====================

    /**
     * Elimina notificaciones antiguas (más de X días)
     * 
     * @param fechaLimite Fecha límite (notificaciones antes de esta fecha serán eliminadas)
     * @return Número de registros eliminados
     */
    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.fechaEnvio < :fechaLimite AND n.leida = true")
    int eliminarNotificacionesAntiguas(@Param("fechaLimite") LocalDateTime fechaLimite);
}
