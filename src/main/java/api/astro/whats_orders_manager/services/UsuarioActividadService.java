package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.UsuarioActividad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * USUARIO ACTIVIDAD SERVICE
 * WhatsApp Orders Manager
 * ============================================================================
 * Servicio para gestión de actividades de usuarios.
 * Maneja el registro y consulta de auditoría.
 * 
 * @version 1.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
public interface UsuarioActividadService {
    
    // ==================== CRUD BÁSICO ====================
    
    List<UsuarioActividad> findAll();
    Page<UsuarioActividad> findAll(Pageable pageable);
    Optional<UsuarioActividad> findById(Long id);
    UsuarioActividad save(UsuarioActividad actividad);
    
    // ==================== REGISTRO DE ACTIVIDADES ====================
    
    /**
     * Registra una actividad simple de un usuario
     * @param idUsuario ID del usuario
     * @param tipo Tipo de actividad (LOGIN, LOGOUT, CREAR_FACTURA, etc.)
     * @param descripcion Descripción de la actividad
     */
    void registrarActividad(Integer idUsuario, String tipo, String descripcion);
    
    /**
     * Registra una actividad relacionada a una entidad
     * @param idUsuario ID del usuario
     * @param tipo Tipo de actividad
     * @param descripcion Descripción
     * @param entidad Nombre de la entidad (FACTURA, CLIENTE, etc.)
     * @param idEntidad ID de la entidad
     */
    void registrarActividad(Integer idUsuario, String tipo, String descripcion, 
                          String entidad, Integer idEntidad);
    
    /**
     * Registra una actividad con metadata JSON adicional
     * @param idUsuario ID del usuario
     * @param tipo Tipo de actividad
     * @param descripcion Descripción
     * @param entidad Nombre de la entidad
     * @param idEntidad ID de la entidad
     * @param metadata JSON con datos adicionales
     * @param nivel Nivel de importancia (INFO, WARNING, CRITICAL)
     */
    void registrarActividadCompleta(Integer idUsuario, String tipo, String descripcion,
                                   String entidad, Integer idEntidad, String metadata, String nivel);
    
    /**
     * Registra una actividad fallida
     * @param idUsuario ID del usuario
     * @param tipo Tipo de actividad
     * @param descripcion Descripción
     * @param errorMensaje Mensaje de error
     */
    void registrarActividadFallida(Integer idUsuario, String tipo, String descripcion, String errorMensaje);
    
    /**
     * Registra un login exitoso
     * @param idUsuario ID del usuario
     * @param ipAddress Dirección IP
     * @param userAgent User Agent del navegador
     */
    void registrarLogin(Integer idUsuario, String ipAddress, String userAgent);
    
    /**
     * Registra un login fallido
     * @param telefono Teléfono del usuario que intentó login
     * @param ipAddress Dirección IP
     * @param motivo Motivo del fallo
     */
    void registrarLoginFallido(String telefono, String ipAddress, String motivo);
    
    /**
     * Registra un logout
     * @param idUsuario ID del usuario
     */
    void registrarLogout(Integer idUsuario);
    
    // ==================== BÚSQUEDAS ====================
    
    List<UsuarioActividad> findByUsuario(Integer idUsuario);
    Page<UsuarioActividad> findByUsuario(Integer idUsuario, Pageable pageable);
    List<UsuarioActividad> findByTipoActividad(String tipo);
    List<UsuarioActividad> findByNivel(String nivel);
    List<UsuarioActividad> findByResultado(String resultado);
    
    /**
     * Busca actividades de un usuario entre fechas
     */
    List<UsuarioActividad> findByUsuarioAndFechas(Integer idUsuario, 
                                                   LocalDateTime fechaInicio, 
                                                   LocalDateTime fechaFin);
    
    /**
     * Busca actividades relacionadas a una entidad
     */
    List<UsuarioActividad> findByEntidad(String entidad, Integer idEntidad);
    
    /**
     * Busca actividades de un usuario sobre una entidad
     */
    List<UsuarioActividad> findByUsuarioAndEntidad(Integer idUsuario, String entidad, Integer idEntidad);
    
    // ==================== SEGURIDAD ====================
    
    /**
     * Busca actividades críticas
     */
    List<UsuarioActividad> findActividadesCriticas();
    
    /**
     * Busca actividades fallidas de un usuario
     */
    List<UsuarioActividad> findActividadesFallidas(Integer idUsuario);
    
    /**
     * Busca actividades sospechosas (múltiples intentos fallidos recientes)
     */
    List<UsuarioActividad> findActividadesSospechosas(int horasAtras, int intentosMinimos);
    
    /**
     * Busca actividades por IP
     */
    List<UsuarioActividad> findByIpAddress(String ipAddress);
    
    // ==================== ESTADÍSTICAS ====================
    
    long countByUsuario(Integer idUsuario);
    long countByTipoActividad(String tipo);
    long countByNivel(String nivel);
    long countByResultado(String resultado);
    long countByUsuarioAndTipo(Integer idUsuario, String tipo);
    
    // ==================== REPORTES ====================
    
    /**
     * Obtiene las últimas N actividades de un usuario
     */
    List<UsuarioActividad> getUltimasActividades(Integer idUsuario, int limite);
    
    /**
     * Obtiene las actividades recientes del sistema
     */
    List<UsuarioActividad> getActividadesRecientes(int limite);
    
    /**
     * Obtiene actividades del día actual
     */
    List<UsuarioActividad> getActividadesHoy();
    
    /**
     * Obtiene actividades de la última semana
     */
    List<UsuarioActividad> getActividadesUltimaSemana();
}
