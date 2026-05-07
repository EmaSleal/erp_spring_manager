package api.astro.whats_orders_manager.modules.configuracion.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;

import java.util.List;
import java.util.Optional;

/**
 * Repository para Provincias de Costa Rica
 */
@Repository
public interface ProvinciaCostaRicaRepository extends JpaRepository<ProvinciaCostaRica, String> {
    
    /**
     * Buscar provincias ordenadas por código
     */
    List<ProvinciaCostaRica> findAllByOrderByCodigo();
    
    /**
     * Buscar provincia por nombre (búsqueda exacta)
     */
    ProvinciaCostaRica findByNombre(String nombre);

    /**
     * Buscar provincia por código
     */
    Optional<ProvinciaCostaRica> findByCodigo(String codigo);

    /**
     * Verificar existencia de provincia por código
     */
    boolean existsByCodigo(String codigo);
}
