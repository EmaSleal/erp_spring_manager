package api.astro.whats_orders_manager.modules.seguridad.service;

import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ============================================================================
 * SERVICIO DE PERMISOS
 * ERP Orders Manager
 * ============================================================================
 * Define las operaciones para verificar permisos de usuarios.
 * Se integra con Spring Security para control de acceso basado en roles.
 * Ahora incluye métodos para trabajar con permisos desde base de datos.
 * 
 * @version 2.0 - Sprint 4 (Migrado a BD)
 * @since 23/12/2025
 * ============================================================================
 */
public interface PermisoService {
    
    /**
     * Verifica si un usuario tiene un permiso específico
     */
    boolean tienePermiso(Usuario usuario, Permiso permiso);
    
    /**
     * Verifica si un usuario tiene un permiso específico (por ID)
     */
    boolean tienePermiso(Integer idUsuario, Permiso permiso);
    
    /**
     * Verifica si un usuario tiene un permiso específico (por username)
     * Útil para integrarse con Spring Security @PreAuthorize
     */
    boolean tienePermisoByUsername(String username, Permiso permiso);
    
    /**
     * Verifica si un usuario tiene al menos uno de los permisos especificados
     */
    boolean tieneAlgunPermiso(Usuario usuario, Permiso... permisos);
    
    /**
     * Verifica si un usuario tiene al menos uno de los permisos especificados (por username)
     */
    boolean tieneAlgunPermisoByUsername(String username, Permiso... permisos);
    
    /**
     * Verifica si un usuario tiene todos los permisos especificados
     */
    boolean tieneTodosLosPermisos(Usuario usuario, Permiso... permisos);
    
    /**
     * Obtiene todos los permisos de un usuario
     */
    Set<Permiso> obtenerPermisos(Usuario usuario);
    
    /**
     * Obtiene todos los permisos de un usuario agrupados por categoría
     */
    Map<String, List<Permiso>> obtenerPermisosPorCategoria(Usuario usuario);
    
    /**
     * Verifica si un usuario puede acceder a un recurso
     */
    boolean puedeAcceder(Usuario usuario, String recurso);
    
    /**
     * Verifica si el usuario está activo y no bloqueado
     */
    boolean esUsuarioValido(Usuario usuario);
    
    /**
     * Obtiene la lista de permisos críticos de un usuario
     */
    Set<Permiso> obtenerPermisosCriticos(Usuario usuario);
    
    /**
     * Compara permisos entre dos usuarios
     */
    Map<String, Object> compararPermisos(Usuario usuario1, Usuario usuario2);
    
    /**
     * Obtiene estadísticas de permisos del sistema
     */
    Map<String, Object> obtenerEstadisticasPermisos();
    
    // ===== MÉTODOS PARA TRABAJAR CON LA BASE DE DATOS =====
    
    /**
     * Obtiene todos los permisos activos desde la BD
     */
    List<api.astro.whats_orders_manager.modules.seguridad.model.Permiso> obtenerTodosActivos();
    
    /**
     * Obtiene todos los permisos (incluyendo inactivos) desde la BD
     */
    List<api.astro.whats_orders_manager.modules.seguridad.model.Permiso> obtenerTodos();
    
    /**
     * Busca un permiso por su código
     */
    Optional<api.astro.whats_orders_manager.modules.seguridad.model.Permiso> buscarPorCodigo(String codigo);
    
    /**
     * Busca un permiso por su ID
     */
    Optional<api.astro.whats_orders_manager.modules.seguridad.model.Permiso> buscarPorId(Long id);
    
    /**
     * Guarda o actualiza un permiso en la BD
     */
    api.astro.whats_orders_manager.modules.seguridad.model.Permiso guardar(api.astro.whats_orders_manager.modules.seguridad.model.Permiso permiso);
    
    // ===== MÉTODOS PARA MIGRACIÓN A BD (basados en código String) =====
    
    /**
     * Verifica si un usuario tiene un permiso por su código (String).
     * Este método reemplaza la verificación basada en enums.
     * Considera permisos del rol Y permisos personalizados del usuario.
     * 
     * @param username Nombre de usuario o teléfono
     * @param codigoPermiso Código del permiso (ej: "FACTURA_VER", "CLIENTE_CREAR")
     * @return true si el usuario tiene el permiso (rol o personalizado)
     */
    boolean tienePermisoPorCodigo(String username, String codigoPermiso);
    
    /**
     * Verifica si un usuario tiene al menos uno de los permisos por código.
     * Versión basada en String para múltiples permisos.
     * 
     * @param username Nombre de usuario o teléfono
     * @param codigosPermiso Códigos de permisos a verificar
     * @return true si tiene al menos uno
     */
    boolean tieneAlgunPermisoPorCodigo(String username, String... codigosPermiso);
}

