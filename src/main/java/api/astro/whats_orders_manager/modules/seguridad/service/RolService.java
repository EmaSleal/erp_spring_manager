package api.astro.whats_orders_manager.modules.seguridad.service;

import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ============================================================================
 * SERVICIO DE ROLES
 * ERP Orders Manager
 * ============================================================================
 * Gestión de roles y sus permisos asociados.
 * 
 * @version 1.0 - Sprint 4 Fase 4.6
 * @since 23/12/2025
 * ============================================================================
 */
public interface RolService {
    
    /**
     * Obtiene todos los roles activos
     */
    List<Rol> obtenerTodosActivos();
    
    /**
     * Obtiene todos los roles (incluyendo inactivos)
     */
    List<Rol> obtenerTodos();
    
    /**
     * Busca un rol por su código
     */
    Optional<Rol> buscarPorCodigo(String codigo);
    
    /**
     * Busca un rol por su ID
     */
    Optional<Rol> buscarPorId(Long id);
    
    /**
     * Obtiene un rol con sus permisos cargados
     */
    Optional<Rol> buscarPorCodigoConPermisos(String codigo);
    
    /**
     * Crea un nuevo rol
     */
    Rol crearRol(String codigo, String nombre, String descripcion);
    
    /**
     * Actualiza un rol existente
     */
    Rol actualizarRol(Long id, String nombre, String descripcion);
    
    /**
     * Activa o desactiva un rol
     */
    Rol cambiarEstado(Long id, boolean activo);
    
    /**
     * Asigna un permiso a un rol
     */
    Rol asignarPermiso(Long idRol, Long idPermiso);
    
    /**
     * Remueve un permiso de un rol
     */
    Rol removerPermiso(Long idRol, Long idPermiso);
    
    /**
     * Asigna múltiples permisos a un rol (reemplaza los existentes)
     */
    Rol asignarPermisos(Long idRol, Set<Long> idsPermisos);
    
    /**
     * Obtiene todos los permisos de un rol
     */
    Set<Permiso> obtenerPermisos(Long idRol);
    
    /**
     * Obtiene todos los permisos de un rol por código
     */
    Set<Permiso> obtenerPermisosPorCodigo(String codigoRol);
    
    /**
     * Verifica si un rol tiene un permiso específico
     */
    boolean tienePermiso(String codigoRol, String codigoPermiso);
    
    /**
     * Cuenta cuántos usuarios tienen cada rol
     */
    List<Object[]> contarUsuariosPorRol();

    /**
     * Encuentra roles por su estado activo
     */
    List<Rol> findByActivoTrue();
}
