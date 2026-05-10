package api.astro.whats_orders_manager.modules.seguridad.service;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * USUARIO SERVICE - Gestión de Usuarios
 * WhatsApp Orders Manager
 * ============================================================================
 * Servicio para la gestión completa de usuarios del sistema.
 * 
 * Funcionalidades:
 * - CRUD básico de usuarios
 * - Búsquedas por teléfono/email
 * - Gestión de bloqueos y desbloqueos
 * - Control de intentos fallidos de login
 * - Cambio de roles
 * - Activación/desactivación de usuarios
 * 
 * @version 2.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
public interface UsuarioService {
    
    // ==================== CRUD BÁSICO ====================
    
    List<Usuario> findAll();
    
    Page<Usuario> findAll(Pageable pageable);
    
    Optional<Usuario> findById(Integer id);
    
    Usuario save(Usuario usuario);
    
    void deleteById(Integer id);
    
    // ==================== BÚSQUEDAS ====================
    
    Optional<Usuario> findByTelefono(String telefono);
    
    Optional<Usuario> findByEmail(String email);
    
    List<Usuario> findByRol(String rol);
    
    List<Usuario> findByActivo(Boolean activo);
    
    List<Usuario> findByBloqueado(Boolean bloqueado);
    
    // ==================== GESTIÓN ADMIN ====================
    
    /**
     * Bloquea un usuario especificando la razón
     */
    Usuario bloquearUsuario(Integer idUsuario, String razon, Integer adminId);
    
    /**
     * Desbloquea un usuario
     */
    Usuario desbloquearUsuario(Integer idUsuario);
    
    /**
     * Cambia el rol de un usuario
     */
    Usuario cambiarRol(Integer idUsuario, String nuevoRol);
    
    /**
     * Activa o desactiva un usuario
     */
    Usuario cambiarEstadoActivo(Integer idUsuario, Boolean activo);
    
    // ==================== SEGURIDAD ====================
    
    void incrementarIntentosFallidos(Integer idUsuario);
    
    void resetearIntentosFallidos(Integer idUsuario);
    
    void actualizarUltimoAcceso(Integer idUsuario);
    
    void forzarCambioPassword(Integer idUsuario);
    
    // ==================== ESTADÍSTICAS ====================
    
    long count();
    
    long countByActivo(Boolean activo);
    
    long countByBloqueado(Boolean bloqueado);
    
    long countByRol(String rol);
}
