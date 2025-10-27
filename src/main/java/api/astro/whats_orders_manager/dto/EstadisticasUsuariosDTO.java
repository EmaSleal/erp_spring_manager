package api.astro.whats_orders_manager.dto;

/**
 * DTO para estadísticas de usuarios
 * Contiene información agregada sobre el estado de los usuarios del sistema
 * 
 * @param total Total de usuarios registrados
 * @param activos Usuarios con estado activo
 * @param inactivos Usuarios con estado inactivo
 * @param administradores Usuarios con rol ADMIN
 * @param vendedores Usuarios con rol VENDEDOR
 * 
 * @version 1.0
 * @since 26/10/2025
 */
public record EstadisticasUsuariosDTO(
    long total,
    long activos,
    long inactivos,
    long administradores,
    long vendedores
) {
    /**
     * Calcula el porcentaje de usuarios activos
     */
    public double porcentajeActivos() {
        return total > 0 ? (activos * 100.0) / total : 0.0;
    }
    
    /**
     * Calcula el porcentaje de administradores
     */
    public double porcentajeAdministradores() {
        return total > 0 ? (administradores * 100.0) / total : 0.0;
    }
}
