package api.astro.whats_orders_manager.modules.configuracion.repository;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para gestionar la configuración de email/SMTP
 * La tabla tiene exactamente una fila (ADR-027)
 */
@Repository
public interface ConfiguracionEmailRepository extends JpaRepository<ConfiguracionEmail, Integer> {
    
    /**
     * La configuración de email (fila única, ADR-027). Único lector: solo lo usa
     * ConfiguracionEmailService.obtenerConfiguracion(); el orden por id lo hace determinista aun si la
     * restricción de fila única faltara.
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionEmail> findFirstByOrderByIdConfiguracionAsc();
    
    /**
     * Verifica si existe alguna configuración de email
     * @return true si existe al menos una configuración
     */
    boolean existsByIdConfiguracionIsNotNull();
}
