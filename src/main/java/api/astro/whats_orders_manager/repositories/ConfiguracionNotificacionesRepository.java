package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.ConfiguracionNotificaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * CONFIGURACIÓN NOTIFICACIONES REPOSITORY
 * WhatsApp Orders Manager
 * ============================================================================
 * Repositorio para acceder a la configuración de notificaciones del sistema.
 * 
 * Solo debe existir un registro activo en la base de datos.
 * ============================================================================
 */
@Repository
public interface ConfiguracionNotificacionesRepository extends JpaRepository<ConfiguracionNotificaciones, Integer> {

    /**
     * Busca la configuración de notificaciones activa
     * Solo debe existir un registro con activo = true
     * 
     * @return Optional con la configuración activa
     */
    @Query("SELECT c FROM ConfiguracionNotificaciones c WHERE c.activo = true")
    Optional<ConfiguracionNotificaciones> findConfiguracionActiva();

    /**
     * Verifica si existe una configuración activa
     * 
     * @return true si existe al menos una configuración activa
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ConfiguracionNotificaciones c WHERE c.activo = true")
    boolean existeConfiguracionActiva();

    /**
     * Cuenta cuántas configuraciones activas existen
     * Solo debería retornar 1
     * 
     * @return cantidad de configuraciones activas
     */
    @Query("SELECT COUNT(c) FROM ConfiguracionNotificaciones c WHERE c.activo = true")
    long contarConfiguracionesActivas();
}
