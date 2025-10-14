package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.ConfiguracionFacturacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad ConfiguracionFacturacion
 * Maneja las operaciones de base de datos para la configuración de facturación.
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 2 - Fase 2
 */
@Repository
public interface ConfiguracionFacturacionRepository extends JpaRepository<ConfiguracionFacturacion, Integer> {

    /**
     * Busca la configuración de facturación activa
     * Solo debe existir una configuración activa
     * 
     * @return Optional con la configuración activa si existe
     */
    @Query("SELECT c FROM ConfiguracionFacturacion c WHERE c.activo = true")
    Optional<ConfiguracionFacturacion> findConfiguracionActiva();

    /**
     * Verifica si existe una configuración activa
     * 
     * @return true si existe al menos una configuración activa
     */
    @Query("SELECT COUNT(c) > 0 FROM ConfiguracionFacturacion c WHERE c.activo = true")
    boolean existeConfiguracionActiva();

    /**
     * Busca configuración por serie de factura
     * 
     * @param serie La serie a buscar
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionFacturacion> findBySerieFactura(String serie);

    /**
     * Cuenta configuraciones activas
     * Debe retornar siempre 1 (validación de integridad)
     * 
     * @return número de configuraciones activas
     */
    @Query("SELECT COUNT(c) FROM ConfiguracionFacturacion c WHERE c.activo = true")
    long contarConfiguracionesActivas();
}
