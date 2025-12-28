package api.astro.whats_orders_manager.modules.configuracion.repository;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para gestionar la configuración de la empresa
 * Solo debe existir un registro de configuración en el sistema
 */
@Repository
public interface ConfiguracionEmpresaRepository extends JpaRepository<ConfiguracionEmpresa, Integer> {
    
    /**
     * Obtiene la primera configuración de empresa (única en el sistema)
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionEmpresa> findFirstByOrderByIdConfiguracionAsc();
    
    /**
     * Verifica si existe alguna configuración de empresa
     * @return true si existe al menos una configuración
     */
    boolean existsByIdConfiguracionIsNotNull();
}
