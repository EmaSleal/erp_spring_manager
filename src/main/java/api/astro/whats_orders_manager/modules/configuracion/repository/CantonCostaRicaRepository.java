package api.astro.whats_orders_manager.modules.configuracion.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import api.astro.whats_orders_manager.modules.configuracion.model.CantonCostaRica;

import java.util.List;

/**
 * Repository para Cantones de Costa Rica
 */
@Repository
public interface CantonCostaRicaRepository extends JpaRepository<CantonCostaRica, Integer> {
    
    /**
     * Buscar cantones por provincia, ordenados por código
     */
    List<CantonCostaRica> findByProvinciaCodigoOrderByCodigo(String provinciaCodigo);
    
    /**
     * Buscar cantón específico por provincia y código
     */
    CantonCostaRica findByProvinciaCodigoAndCodigo(String provinciaCodigo, String codigo);
    
    /**
     * Buscar cantones por nombre (búsqueda parcial)
     */
    @Query("SELECT c FROM CantonCostaRica c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) ORDER BY c.provinciaCodigo, c.codigo")
    List<CantonCostaRica> findByNombreContaining(@Param("nombre") String nombre);
    
    /**
     * Contar cantones por provincia
     */
    long countByProvinciaCodigo(String provinciaCodigo);
}
