package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.models.UsuarioPermiso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad UsuarioPermiso
 */
@Repository
public interface UsuarioPermisoRepository extends JpaRepository<UsuarioPermiso, Long> {

    /**
     * Busca todos los permisos personalizados de un usuario
     */
    List<UsuarioPermiso> findByUsuario(Usuario usuario);

    /**
     * Busca todos los permisos personalizados de un usuario por ID
     */
    List<UsuarioPermiso> findByUsuario_IdUsuario(Integer idUsuario);

    /**
     * Busca un permiso personalizado específico de un usuario
     */
    @Query("SELECT up FROM UsuarioPermiso up WHERE up.usuario.idUsuario = :idUsuario " +
            "AND up.permiso.codigo = :codigoPermiso")
    Optional<UsuarioPermiso> findByUsuarioAndPermisoCodigo(
            @Param("idUsuario") Integer idUsuario,
            @Param("codigoPermiso") String codigoPermiso
    );

    /**
     * Busca todos los permisos concedidos de un usuario
     */
    @Query("SELECT up FROM UsuarioPermiso up WHERE up.usuario.idUsuario = :idUsuario " +
            "AND up.tipo = 'CONCEDIDO'")
    List<UsuarioPermiso> findPermisosConcedidos(@Param("idUsuario") Integer idUsuario);

    /**
     * Busca todos los permisos denegados de un usuario
     */
    @Query("SELECT up FROM UsuarioPermiso up WHERE up.usuario.idUsuario = :idUsuario " +
            "AND up.tipo = 'DENEGADO'")
    List<UsuarioPermiso> findPermisosDenegados(@Param("idUsuario") Integer idUsuario);

    /**
     * Elimina un permiso personalizado específico
     */
    @Query("DELETE FROM UsuarioPermiso up WHERE up.usuario.idUsuario = :idUsuario " +
            "AND up.permiso.codigo = :codigoPermiso")
    void deleteByUsuarioAndPermisoCodigo(
            @Param("idUsuario") Integer idUsuario,
            @Param("codigoPermiso") String codigoPermiso
    );

    /**
     * Cuenta permisos personalizados por usuario
     */
    @Query("SELECT u.nombre, COUNT(up) FROM UsuarioPermiso up " +
            "JOIN up.usuario u GROUP BY u.nombre")
    List<Object[]> contarPermisosPorUsuario();
}
