package api.astro.whats_orders_manager.modules.seguridad.service;

import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioPermiso;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para gestión de permisos personalizados por usuario.
 * Permite conceder o denegar permisos específicos que sobrescriben los del rol.
 */
public interface UsuarioPermisoService {

    /**
     * Concede un permiso adicional a un usuario (aunque su rol no lo tenga)
     * 
     * @param idUsuario Usuario al que se concede el permiso
     * @param idPermiso Permiso a conceder
     * @param concedidoPorId Usuario que concede el permiso
     * @return UsuarioPermiso creado
     */
    UsuarioPermiso concederPermiso(Integer idUsuario, Long idPermiso, Integer concedidoPorId);

    /**
     * Deniega un permiso a un usuario (aunque su rol lo tenga)
     * 
     * @param idUsuario Usuario al que se deniega el permiso
     * @param idPermiso Permiso a denegar
     * @param concedidoPorId Usuario que deniega el permiso
     * @return UsuarioPermiso creado
     */
    UsuarioPermiso denegarPermiso(Integer idUsuario, Long idPermiso, Integer concedidoPorId);

    /**
     * Remueve un permiso personalizado (vuelve a usar los del rol)
     * 
     * @param idUsuario Usuario
     * @param codigoPermiso Código del permiso a remover
     */
    void removerPermisoPersonalizado(Integer idUsuario, String codigoPermiso);

    /**
     * Obtiene todos los permisos personalizados de un usuario
     * 
     * @param idUsuario ID del usuario
     * @return Lista de permisos personalizados
     */
    List<UsuarioPermiso> obtenerPermisosPersonalizados(Integer idUsuario);

    /**
     * Obtiene los permisos efectivos de un usuario (rol + personalizados)
     * Aplica la lógica: denegados > concedidos > rol
     * 
     * @param idUsuario ID del usuario
     * @return Map con código del permiso y si está activo
     */
    Map<String, Boolean> obtenerPermisosEfectivos(Integer idUsuario);

    /**
     * Obtiene los permisos concedidos adicionales (que el rol no tiene)
     * 
     * @param idUsuario ID del usuario
     * @return Lista de permisos concedidos
     */
    List<UsuarioPermiso> obtenerPermisosConcedidos(Integer idUsuario);

    /**
     * Obtiene los permisos denegados (que el rol tiene pero se niegan)
     * 
     * @param idUsuario ID del usuario
     * @return Lista de permisos denegados
     */
    List<UsuarioPermiso> obtenerPermisosDenegados(Integer idUsuario);

    /**
     * Busca un permiso personalizado específico
     * 
     * @param idUsuario ID del usuario
     * @param codigoPermiso Código del permiso
     * @return Optional con el permiso si existe
     */
    Optional<UsuarioPermiso> buscarPermisoPersonalizado(Integer idUsuario, String codigoPermiso);

    /**
     * Verifica si un usuario tiene un permiso específico (considerando personalizaciones)
     * 
     * @param idUsuario ID del usuario
     * @param codigoPermiso Código del permiso
     * @return true si tiene el permiso
     */
    boolean tienePermiso(Integer idUsuario, String codigoPermiso);

    /**
     * Cuenta permisos personalizados por usuario
     * 
     * @return Map con nombre de usuario y cantidad de permisos personalizados
     */
    Map<String, Long> contarPermisosPorUsuario();

    /**
     * Obtiene un resumen de permisos para mostrar en UI
     * Incluye: permisos del rol, concedidos adicionales, denegados
     * 
     * @param idUsuario ID del usuario
     * @return Map con categorías y listas de permisos
     */
    Map<String, Object> obtenerResumenPermisos(Integer idUsuario);
}
