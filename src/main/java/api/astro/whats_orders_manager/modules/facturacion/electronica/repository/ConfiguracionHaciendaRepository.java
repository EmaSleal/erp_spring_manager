package api.astro.whats_orders_manager.modules.facturacion.electronica.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para configuraciones de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Repository
public interface ConfiguracionHaciendaRepository extends JpaRepository<ConfiguracionHacienda, Long> {
    
    /**
     * Encuentra la configuración activa de una empresa.
     */
    @Query("SELECT c FROM ConfiguracionHacienda c WHERE c.empresa.id = :empresaId AND c.activa = true")
    Optional<ConfiguracionHacienda> findActivaByEmpresaId(@Param("empresaId") Long empresaId);
    
    /**
     * Encuentra configuración por empresa y ambiente.
     */
    @Query("SELECT c FROM ConfiguracionHacienda c WHERE c.empresa.id = :empresaId AND c.ambiente = :ambiente")
    Optional<ConfiguracionHacienda> findByEmpresaIdAndAmbiente(
        @Param("empresaId") Long empresaId,
        @Param("ambiente") AmbienteHacienda ambiente
    );
    
    /**
     * Verifica si existe configuración activa para una empresa.
     */
    @Query("SELECT COUNT(c) > 0 FROM ConfiguracionHacienda c WHERE c.empresa.id = :empresaId AND c.activa = true")
    boolean existsActivaByEmpresaId(@Param("empresaId") Long empresaId);
    
    /**
     * Encuentra todas las configuraciones de una empresa.
     */
    @Query("SELECT c FROM ConfiguracionHacienda c WHERE c.empresa.id = :empresaId ORDER BY c.activa DESC, c.createdAt DESC")
    java.util.List<ConfiguracionHacienda> findAllByEmpresaId(@Param("empresaId") Long empresaId);
}
