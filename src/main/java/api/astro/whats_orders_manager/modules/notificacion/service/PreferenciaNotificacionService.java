package api.astro.whats_orders_manager.modules.notificacion.service;

import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * PREFERENCIA NOTIFICACIÓN SERVICE
 * WhatsApp Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Servicio para gestionar preferencias individuales de notificaciones.
 * 
 * Funcionalidades:
 * - Configurar preferencias por usuario, tipo y canal
 * - Preferencias globales (todos los tipos/canales)
 * - Preferencias por tipo (todos los canales)
 * - Preferencias por canal (todos los tipos)
 * - Preferencias específicas (tipo + canal)
 * - Validación de permisos de envío
 * ============================================================================
 */
public interface PreferenciaNotificacionService {

    // ==================== CRUD ====================

    /**
     * Obtiene una preferencia por ID
     * 
     * @param idPreferencia ID de la preferencia
     * @return Optional con la preferencia
     */
    Optional<PreferenciaNotificacion> findById(Integer idPreferencia);

    /**
     * Obtiene todas las preferencias de un usuario
     * 
     * @param usuario Usuario propietario
     * @return Lista de preferencias
     */
    List<PreferenciaNotificacion> findByUsuario(Usuario usuario);

    /**
     * Obtiene todas las preferencias de un usuario (paginadas)
     * 
     * @param usuario Usuario propietario
     * @param pageable Paginación
     * @return Page con preferencias
     */
    Page<PreferenciaNotificacion> findByUsuario(Usuario usuario, Pageable pageable);

    /**
     * Crea una nueva preferencia
     * 
     * @param preferencia Preferencia a crear
     * @return Preferencia creada
     */
    PreferenciaNotificacion crear(PreferenciaNotificacion preferencia);

    /**
     * Actualiza una preferencia existente
     * 
     * @param idPreferencia ID de la preferencia
     * @param preferencia Datos actualizados
     * @return Preferencia actualizada
     */
    PreferenciaNotificacion actualizar(Integer idPreferencia, PreferenciaNotificacion preferencia);

    /**
     * Elimina una preferencia
     * 
     * @param idPreferencia ID de la preferencia
     * @return true si se eliminó correctamente
     */
    boolean eliminar(Integer idPreferencia);

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================

    /**
     * Obtiene preferencia específica de usuario + tipo + canal
     * 
     * @param usuario Usuario propietario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Optional con la preferencia específica
     */
    Optional<PreferenciaNotificacion> findByUsuarioTipoCanal(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal
    );

    /**
     * Obtiene preferencias por tipo (para todos los canales)
     * 
     * @param usuario Usuario propietario
     * @param tipo Tipo de notificación
     * @return Lista de preferencias del tipo
     */
    List<PreferenciaNotificacion> findByUsuarioAndTipo(Usuario usuario, TipoNotificacion tipo);

    /**
     * Obtiene preferencias por canal (para todos los tipos)
     * 
     * @param usuario Usuario propietario
     * @param canal Canal de notificación
     * @return Lista de preferencias del canal
     */
    List<PreferenciaNotificacion> findByUsuarioAndCanal(Usuario usuario, CanalNotificacion canal);

    /**
     * Obtiene preferencia global del usuario (tipo=NULL, canal=NULL)
     * 
     * @param usuario Usuario propietario
     * @return Optional con la preferencia global
     */
    Optional<PreferenciaNotificacion> findPreferenciaGlobal(Usuario usuario);

    /**
     * Obtiene preferencias activas de un usuario
     * 
     * @param usuario Usuario propietario
     * @return Lista de preferencias activas
     */
    List<PreferenciaNotificacion> findActivas(Usuario usuario);

    /**
     * Obtiene preferencias aplicables para un tipo y canal específicos
     * Ordenadas por especificidad (específica > tipo > canal > global)
     * 
     * @param usuario Usuario propietario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Lista de preferencias aplicables
     */
    List<PreferenciaNotificacion> findPreferenciasAplicables(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal
    );

    // ==================== VALIDACIÓN DE PERMISOS ====================

    /**
     * Verifica si un usuario acepta recibir un tipo de notificación por un canal
     * Aplica lógica de prioridad: específica > tipo > canal > global
     * 
     * @param usuario Usuario a validar
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return true si acepta, false si rechaza
     */
    boolean usuarioAceptaNotificacion(Usuario usuario, TipoNotificacion tipo, CanalNotificacion canal);

    /**
     * Verifica si un usuario acepta notificaciones por ID
     * 
     * @param idUsuario ID del usuario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return true si acepta, false si rechaza
     */
    boolean usuarioAceptaNotificacionPorId(Integer idUsuario, TipoNotificacion tipo, CanalNotificacion canal);

    /**
     * Verifica si un usuario tiene notificaciones desactivadas globalmente
     * 
     * @param usuario Usuario a validar
     * @return true si tiene notificaciones desactivadas
     */
    boolean tieneNotificacionesDesactivadas(Usuario usuario);

    /**
     * Verifica si se puede enviar una notificación ahora (valida horario)
     * 
     * @param preferencia Preferencia a validar
     * @return true si se puede enviar ahora
     */
    boolean sePuedeEnviarAhora(PreferenciaNotificacion preferencia);

    // ==================== OPERACIONES MASIVAS ====================

    /**
     * Activa una preferencia específica
     * 
     * @param idPreferencia ID de la preferencia
     * @return true si se activó correctamente
     */
    boolean activar(Integer idPreferencia);

    /**
     * Desactiva una preferencia específica
     * 
     * @param idPreferencia ID de la preferencia
     * @return true si se desactivó correctamente
     */
    boolean desactivar(Integer idPreferencia);

    /**
     * Desactiva TODAS las notificaciones de un usuario (global)
     * 
     * @param usuario Usuario propietario
     * @return Cantidad de preferencias desactivadas
     */
    int desactivarTodasLasNotificaciones(Usuario usuario);

    /**
     * Reactiva TODAS las notificaciones de un usuario (global)
     * 
     * @param usuario Usuario propietario
     * @return Cantidad de preferencias activadas
     */
    int reactivarTodasLasNotificaciones(Usuario usuario);

    /**
     * Desactiva notificaciones de un tipo específico (para todos los canales)
     * 
     * @param usuario Usuario propietario
     * @param tipo Tipo de notificación
     * @return Cantidad de preferencias desactivadas
     */
    int desactivarPorTipo(Usuario usuario, TipoNotificacion tipo);

    /**
     * Desactiva notificaciones de un canal específico (para todos los tipos)
     * 
     * @param usuario Usuario propietario
     * @param canal Canal de notificación
     * @return Cantidad de preferencias desactivadas
     */
    int desactivarPorCanal(Usuario usuario, CanalNotificacion canal);

    // ==================== CONFIGURACIÓN PREDETERMINADA ====================

    /**
     * Crea preferencias predeterminadas para un nuevo usuario
     * Incluye configuración básica para todos los tipos y canales
     * 
     * @param usuario Usuario nuevo
     * @return Lista de preferencias creadas
     */
    List<PreferenciaNotificacion> crearPreferenciasPredeterminadas(Usuario usuario);

    /**
     * Obtiene configuración predeterminada para un tipo y canal
     * 
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @return Preferencia con valores predeterminados (no persistida)
     */
    PreferenciaNotificacion obtenerConfiguracionPredeterminada(TipoNotificacion tipo, CanalNotificacion canal);

    /**
     * Crea o actualiza una preferencia específica
     * Si existe, actualiza; si no existe, crea
     * 
     * @param usuario Usuario propietario
     * @param tipo Tipo de notificación
     * @param canal Canal de notificación
     * @param activa Estado activo/inactivo
     * @return Preferencia creada/actualizada
     */
    PreferenciaNotificacion crearOActualizar(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            Boolean activa
    );

    // ==================== CONFIGURACIÓN DE HORARIOS ====================

    /**
     * Configura horario preferido para notificaciones
     * 
     * @param idPreferencia ID de la preferencia
     * @param horaPreferida Hora en formato "HH:mm"
     * @return true si se configuró correctamente
     */
    boolean configurarHorario(Integer idPreferencia, String horaPreferida);

    /**
     * Configura si solo acepta notificaciones en horario laboral
     * 
     * @param idPreferencia ID de la preferencia
     * @param soloHorarioLaboral true para restricción laboral
     * @return true si se configuró correctamente
     */
    boolean configurarHorarioLaboral(Integer idPreferencia, Boolean soloHorarioLaboral);

    /**
     * Configura frecuencia de notificaciones (INMEDIATA, DIARIA, SEMANAL)
     * 
     * @param idPreferencia ID de la preferencia
     * @param frecuencia Frecuencia de envío
     * @return true si se configuró correctamente
     */
    boolean configurarFrecuencia(Integer idPreferencia, String frecuencia);

    // ==================== ESTADÍSTICAS ====================

    /**
     * Cuenta usuarios con notificaciones activas
     * 
     * @return Cantidad de usuarios
     */
    long contarUsuariosConNotificacionesActivas();

    /**
     * Cuenta usuarios con notificaciones desactivadas globalmente
     * 
     * @return Cantidad de usuarios
     */
    long contarUsuariosConNotificacionesDesactivadas();

    /**
     * Obtiene estadísticas de preferencias por tipo
     * 
     * @return Map con tipo y cantidad de preferencias activas
     */
    Map<TipoNotificacion, Long> obtenerEstadisticasPorTipo();

    /**
     * Obtiene estadísticas de preferencias por canal
     * 
     * @return Map con canal y cantidad de preferencias activas
     */
    Map<CanalNotificacion, Long> obtenerEstadisticasPorCanal();

    /**
     * Cuenta preferencias activas de un usuario
     * 
     * @param usuario Usuario propietario
     * @return Cantidad de preferencias activas
     */
    long contarActivas(Usuario usuario);

    /**
     * Cuenta preferencias totales de un usuario
     * 
     * @param usuario Usuario propietario
     * @return Cantidad total de preferencias
     */
    long contarTotal(Usuario usuario);
}
