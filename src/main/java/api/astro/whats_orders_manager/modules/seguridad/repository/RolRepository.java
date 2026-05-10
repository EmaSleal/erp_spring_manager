package api.astro.whats_orders_manager.modules.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import api.astro.whats_orders_manager.modules.seguridad.model.Rol;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Rol
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    /**
     * Busca un rol por su código único
     */
    Optional<Rol> findByCodigo(String codigo);

    /**
     * Busca todos los roles activos
     */
    List<Rol> findByActivoTrue();

    /**
     * Verifica si existe un rol con el código dado
     */
    boolean existsByCodigo(String codigo);

    /**
     * Busca roles que contengan el texto en nombre o descripción
     */
    @Query("SELECT r FROM Rol r WHERE LOWER(r.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Rol> buscarPorTexto(@Param("texto") String texto);

    /**
     * Obtiene un rol con sus permisos cargados
     */
    @Query("SELECT r FROM Rol r LEFT JOIN FETCH r.permisos WHERE r.codigo = :codigo")
    Optional<Rol> findByCodigoWithPermisos(@Param("codigo") String codigo);

    /**
     * Cuenta usuarios por rol
     */
    @Query("SELECT r.nombre, COUNT(u) FROM Rol r LEFT JOIN r.usuarios u WHERE r.activo = true GROUP BY r.nombre")
    List<Object[]> contarUsuariosPorRol();
}
