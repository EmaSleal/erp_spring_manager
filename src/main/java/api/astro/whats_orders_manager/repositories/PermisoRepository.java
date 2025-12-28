package api.astro.whats_orders_manager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import api.astro.whats_orders_manager.models.Permiso;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Permiso
 */
@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    /**
     * Busca un permiso por su código único
     */
    Optional<Permiso> findByCodigo(String codigo);

    /**
     * Busca todos los permisos activos
     */
    List<Permiso> findByActivoTrue();

    /**
     * Busca permisos por categoría
     */
    List<Permiso> findByCategoria(String categoria);

    /**
     * Busca permisos críticos
     */
    List<Permiso> findByEsCriticoTrue();

    /**
     * Busca permisos por categoría y activos
     */
    List<Permiso> findByCategoriaAndActivoTrue(String categoria);

    /**
     * Verifica si existe un permiso con el código dado
     */
    boolean existsByCodigo(String codigo);

    /**
     * Busca permisos que contengan el texto en nombre o descripción
     */
    @Query("SELECT p FROM Permiso p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Permiso> buscarPorTexto(@Param("texto") String texto);

    /**
     * Cuenta permisos por categoría
     */
    @Query("SELECT p.categoria, COUNT(p) FROM Permiso p WHERE p.activo = true GROUP BY p.categoria")
    List<Object[]> contarPermisosPorCategoria();
}
