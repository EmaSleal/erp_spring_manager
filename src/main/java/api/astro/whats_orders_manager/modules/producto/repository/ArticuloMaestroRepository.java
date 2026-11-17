package api.astro.whats_orders_manager.modules.producto.repository;

import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticuloMaestroRepository extends JpaRepository<ArticuloMaestro, Integer> {

    /**
     * Full-text contains search on descripcion, case-insensitive.
     */
    List<ArticuloMaestro> findByDescripcionContainingIgnoreCase(String descripcion);

    /**
     * Returns true when the given master has at least one Producto variant linked to it.
     */
    @Query("SELECT COUNT(v) > 0 FROM Producto v WHERE v.articuloMaestro.idArticuloMaestro = :id")
    boolean existsVariantesById(@Param("id") Integer id);

    /**
     * Returns variant counts per master article using the mapped OneToMany relationship.
     * Result: Object[] where [0] = idArticuloMaestro (Integer), [1] = count (Long).
     * Used by the list view to avoid lazy-loading the variantes collection in Thymeleaf.
     */
    @Query("SELECT a.idArticuloMaestro, COUNT(v) FROM ArticuloMaestro a LEFT JOIN a.variantes v GROUP BY a.idArticuloMaestro")
    List<Object[]> countVariantesByMaestro();

    /**
     * Active masters without a CAByS (null or blank). SQL equivalent of the counter:
     * SELECT COUNT(*) FROM articulo_maestro WHERE active = 1 AND (codigo_cabys IS NULL OR TRIM(codigo_cabys) = '')
     */
    @Query("SELECT a FROM ArticuloMaestro a WHERE a.active = true AND (a.codigoCabys IS NULL OR TRIM(a.codigoCabys) = '') ORDER BY a.descripcion")
    List<ArticuloMaestro> findActivosSinCabys();

    @Query("SELECT COUNT(a) FROM ArticuloMaestro a WHERE a.active = true AND (a.codigoCabys IS NULL OR TRIM(a.codigoCabys) = '')")
    long countActivosSinCabys();
}
