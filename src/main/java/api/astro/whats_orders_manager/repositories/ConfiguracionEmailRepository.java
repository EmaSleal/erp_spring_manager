package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.ConfiguracionEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para gestionar la configuración de email/SMTP
 * Solo debe existir un registro de configuración en el sistema
 */
@Repository
public interface ConfiguracionEmailRepository extends JpaRepository<ConfiguracionEmail, Integer> {
    
    /**
     * Obtiene la primera configuración de email (única en el sistema)
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionEmail> findFirstByOrderByIdConfiguracionAsc();
    
    /**
     * Obtiene la configuración de email activa
     * @return Optional con la configuración activa
     */
    Optional<ConfiguracionEmail> findFirstByActivoTrue();
    
    /**
     * Verifica si existe alguna configuración de email
     * @return true si existe al menos una configuración
     */
    boolean existsByIdConfiguracionIsNotNull();
}
