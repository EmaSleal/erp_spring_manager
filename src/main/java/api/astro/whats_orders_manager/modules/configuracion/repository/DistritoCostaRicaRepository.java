package api.astro.whats_orders_manager.modules.configuracion.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import api.astro.whats_orders_manager.modules.configuracion.model.DistritoCostaRica;

import java.util.List;

/**
 * Repository para Distritos de Costa Rica
 */
@Repository
public interface DistritoCostaRicaRepository extends JpaRepository<DistritoCostaRica, Integer> {
    
    /**
     * Buscar distritos por provincia, ordenados por cantón y código
     */
    List<DistritoCostaRica> findByProvinciaCodigoOrderByCantonCodigoAscCodigoAsc(String provinciaCodigo);
    
    /**
     * Buscar distritos por provincia y cantón, ordenados por código
     */
    List<DistritoCostaRica> findByProvinciaCodigoAndCantonCodigoOrderByCodigo(String provinciaCodigo, String cantonCodigo);
    
    /**
     * Buscar distrito específico por provincia, cantón y código
     */
    DistritoCostaRica findByProvinciaCodigoAndCantonCodigoAndCodigo(String provinciaCodigo, String cantonCodigo, String codigo);
    
    /**
     * Buscar distritos por nombre (búsqueda parcial)
     */
    @Query("SELECT d FROM DistritoCostaRica d WHERE LOWER(d.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) ORDER BY d.provinciaCodigo, d.cantonCodigo, d.codigo")
    List<DistritoCostaRica> findByNombreContaining(@Param("nombre") String nombre);
    
    /**
     * Contar distritos por provincia y cantón
     */
    long countByProvinciaCodigoAndCantonCodigo(String provinciaCodigo, String cantonCodigo);
}
