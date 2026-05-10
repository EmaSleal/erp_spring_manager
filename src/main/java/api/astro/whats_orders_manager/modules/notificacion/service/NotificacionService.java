package api.astro.whats_orders_manager.modules.notificacion.service;

import api.astro.whats_orders_manager.modules.notificacion.model.Notificacion;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.notificacion.dto.NotificacionDTO;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * NOTIFICACIÓN SERVICE
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Servicio para gestionar el historial y envío de notificaciones.
 * 
 * Funcionalidades:
 * - Enviar notificaciones por diferentes canales (WEB, EMAIL, WHATSAPP)
 * - Gestionar historial de notificaciones
 * - Marcar notificaciones como leídas
 * - Obtener notificaciones pendientes de un usuario
 * - Generar estadísticas de notificaciones
 * ============================================================================
 */
public interface NotificacionService {

    // ==================== ENVÍO DE NOTIFICACIONES ====================

    /**
     * Envía una notificación a un usuario
     * 
     * @param usuario Usuario destinatario
     * @param tipo Tipo de notificación
     * @param canal Canal de envío
     * @param titulo Título de la notificación
     * @param mensaje Contenido del mensaje
     * @return Notificación creada
     */
    Notificacion enviarNotificacion(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String titulo,
            String mensaje
    );

    /**
     * Envía una notificación con URL de acción
     * 
     * @param usuario Usuario destinatario
     * @param tipo Tipo de notificación
     * @param canal Canal de envío
     * @param titulo Título de la notificación
     * @param mensaje Contenido del mensaje
     * @param urlAccion URL de redirección
     * @param textoBoton Texto del botón de acción
     * @return Notificación creada
     */
    Notificacion enviarNotificacionConAccion(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String titulo,
            String mensaje,
            String urlAccion,
            String textoBoton
    );

    /**
     * Envía una notificación usando una plantilla
     * 
     * @param usuario Usuario destinatario
     * @param tipo Tipo de notificación
     * @param canal Canal de envío
     * @param idPlantilla ID de la plantilla a usar
     * @param variables Variables para reemplazar en la plantilla
     * @return Notificación creada
     */
    Notificacion enviarNotificacionConPlantilla(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            Integer idPlantilla,
            Map<String, Object> variables
    );

    /**
     * Envía una notificación a un destinatario externo (no usuario del sistema)
     * 
     * @param emailOTelefono Email o teléfono del destinatario
     * @param tipo Tipo de notificación
     * @param canal Canal de envío
     * @param titulo Título de la notificación
     * @param mensaje Contenido del mensaje
     * @return Notificación creada
     */
    Notificacion enviarNotificacionExterna(
            String emailOTelefono,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String titulo,
            String mensaje
    );

    /**
     * Envía una notificación web (en tiempo real)
     * 
     * @param usuario Usuario destinatario
     * @param tipo Tipo de notificación
     * @param titulo Título de la notificación
     * @param mensaje Contenido del mensaje
     * @return Notificación creada
     */
    Notificacion enviarNotificacionWeb(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje);

    /**
     * Envía una notificación por email
     * 
     * @param usuario Usuario destinatario
     * @param tipo Tipo de notificación
     * @param titulo Título (asunto) del email
     * @param mensaje Contenido del email (puede ser HTML)
     * @return Notificación creada
     */
    Notificacion enviarNotificacionEmail(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje);

    /**
     * Envía una notificación por WhatsApp
     * 
     * @param usuario Usuario destinatario
     * @param tipo Tipo de notificación
     * @param mensaje Contenido del mensaje (solo texto)
     * @return Notificación creada
     */
    Notificacion enviarNotificacionWhatsApp(Usuario usuario, TipoNotificacion tipo, String mensaje);

    // ==================== CONSULTAS ====================

    /**
     * Obtiene una notificación por ID
     * 
     * @param idNotificacion ID de la notificación
     * @return Optional con la notificación
     */
    Optional<Notificacion> findById(Integer idNotificacion);

    /**
     * Obtiene todas las notificaciones de un usuario (paginadas)
     * 
     * @param usuario Usuario destinatario
     * @param pageable Paginación
     * @return Page con notificaciones
     */
    Page<Notificacion> findByUsuario(Usuario usuario, Pageable pageable);

    /**
     * Obtiene notificaciones NO LEÍDAS de un usuario
     * 
     * @param usuario Usuario destinatario
     * @return Lista de notificaciones no leídas
     */
    List<Notificacion> findNoLeidasByUsuario(Usuario usuario);

    /**
     * Obtiene notificaciones NO LEÍDAS por ID de usuario
     * 
     * @param idUsuario ID del usuario
     * @return Lista de notificaciones no leídas
     */
    List<Notificacion> findNoLeidasByUsuarioId(Integer idUsuario);

    /**
     * Obtiene notificaciones NO LEÍDAS por ID de usuario (paginadas)
     * 
     * @param idUsuario ID del usuario
     * @param pageable Paginación
     * @return Page con notificaciones no leídas
     */
    Page<Notificacion> findNoLeidasByUsuarioId(Integer idUsuario, Pageable pageable);

    /**
     * Obtiene notificaciones LEÍDAS por ID de usuario (paginadas)
     * 
     * @param idUsuario ID del usuario
     * @param pageable Paginación
     * @return Page con notificaciones leídas
     */
    Page<Notificacion> findLeidasByUsuarioId(Integer idUsuario, Pageable pageable);

    /**
     * Obtiene todas las notificaciones por ID de usuario (paginadas)
     * 
     * @param idUsuario ID del usuario
     * @param pageable Paginación
     * @return Page con todas las notificaciones
     */
    Page<Notificacion> findByUsuarioId(Integer idUsuario, Pageable pageable);

    /**
     * Obtiene notificaciones por usuario, tipo y estado (paginadas)
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param leida Estado de lectura
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    Page<Notificacion> findByUsuarioIdAndTipoAndLeida(Integer idUsuario, String tipo, Boolean leida, Pageable pageable);

    /**
     * Obtiene notificaciones por usuario y tipo (paginadas)
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param pageable Paginación
     * @return Page con notificaciones filtradas
     */
    Page<Notificacion> findByUsuarioIdAndTipo(Integer idUsuario, String tipo, Pageable pageable);

    /**
     * Cuenta notificaciones NO LEÍDAS de un usuario
     * 
     * @param usuario Usuario destinatario
     * @return Cantidad de notificaciones no leídas
     */
    long countNoLeidasByUsuario(Usuario usuario);

    /**
     * Cuenta notificaciones NO LEÍDAS por ID de usuario
     * 
     * @param idUsuario ID del usuario
     * @return Cantidad de notificaciones no leídas
     */
    long countNoLeidasByUsuarioId(Integer idUsuario);

    /**
     * Obtiene las últimas N notificaciones de un usuario
     * 
     * @param idUsuario ID del usuario
     * @param limite Número máximo de notificaciones
     * @return Lista de notificaciones recientes
     */
    List<Notificacion> findUltimasNotificaciones(Integer idUsuario, Integer limite);

    /**
     * Obtiene notificaciones por tipo y canal (paginadas)
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @param pageable Paginación
     * @return Page con notificaciones
     */
    Page<Notificacion> findByTipoAndCanal(TipoNotificacion tipo, CanalNotificacion canal, Pageable pageable);

    /**
     * Obtiene notificaciones relacionadas con una factura
     * 
     * @param idFactura ID de la factura
     * @return Lista de notificaciones relacionadas
     */
    List<Notificacion> findByFacturaRelacionada(Integer idFactura);

    /**
     * Obtiene notificaciones relacionadas con un cliente
     * 
     * @param idCliente ID del cliente
     * @return Lista de notificaciones relacionadas
     */
    List<Notificacion> findByClienteRelacionado(Integer idCliente);

    // ==================== OPERACIONES ====================

    /**
     * Marca una notificación como leída
     * 
     * @param idNotificacion ID de la notificación
     * @return true si se marcó correctamente
     */
    boolean marcarComoLeida(Integer idNotificacion);

    /**
     * Marca todas las notificaciones de un usuario como leídas
     * 
     * @param idUsuario ID del usuario
     * @return Cantidad de notificaciones marcadas
     */
    int marcarTodasComoLeidas(Integer idUsuario);

    /**
     * Elimina una notificación
     * 
     * @param idNotificacion ID de la notificación
     * @return true si se eliminó correctamente
     */
    boolean eliminar(Integer idNotificacion);

    /**
     * Reintenta enviar notificaciones fallidas
     * 
     * @param maxIntentos Máximo número de intentos permitidos
     * @return Cantidad de notificaciones reintentadas
     */
    int reintentarNotificacionesFallidas(Integer maxIntentos);

    /**
     * Elimina notificaciones antiguas (ya leídas)
     * 
     * @param diasAntiguedad Días de antigüedad para considerar "antigua"
     * @return Cantidad de notificaciones eliminadas
     */
    int eliminarNotificacionesAntiguas(Integer diasAntiguedad);

    // ==================== CONVERSIÓN ====================

    /**
     * Convierte una entidad Notificacion a DTO
     * 
     * @param notificacion Entidad a convertir
     * @return DTO con datos de la notificación
     */
    NotificacionDTO convertirADTO(Notificacion notificacion);

    /**
     * Convierte una lista de entidades a DTOs
     * 
     * @param notificaciones Lista de entidades
     * @return Lista de DTOs
     */
    List<NotificacionDTO> convertirADTOs(List<Notificacion> notificaciones);

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtiene estadísticas de notificaciones por tipo
     * 
     * @return Map con tipo y cantidad
     */
    Map<TipoNotificacion, Long> obtenerEstadisticasPorTipo();

    /**
     * Obtiene estadísticas de notificaciones por canal
     * 
     * @return Map con canal y cantidad
     */
    Map<CanalNotificacion, Long> obtenerEstadisticasPorCanal();

    /**
     * Cuenta notificaciones enviadas exitosamente
     * 
     * @return Cantidad de notificaciones enviadas
     */
    long countEnviadas();

    /**
     * Cuenta notificaciones fallidas
     * 
     * @return Cantidad de notificaciones con error
     */
    long countFallidas();
}
