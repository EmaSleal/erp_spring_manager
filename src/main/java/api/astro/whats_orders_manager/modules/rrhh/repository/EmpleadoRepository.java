package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    List<Empleado> findByActivoTrue();

    Optional<Empleado> findByCedula(String cedula);

    List<Empleado> findByDepartamentoAndActivoTrue(Departamento departamento);

    long countByActivoTrue();

    Page<Empleado> findAll(Pageable pageable);

    /**
     * Find the employee linked to the given system user ID.
     * Uses explicit JPQL to avoid ambiguity with derived-query name traversal.
     */
    @Query("SELECT e FROM Empleado e WHERE e.usuario.idUsuario = :usuarioId")
    Optional<Empleado> findByUsuarioId(@Param("usuarioId") Integer usuarioId);

    /**
     * Find the employee linked to the user with the given email (Spring Security principal name).
     */
    @Query("SELECT e FROM Empleado e WHERE e.usuario.email = :email")
    Optional<Empleado> findByUsuarioEmail(@Param("email") String email);

    /**
     * Returns all employee usuario IDs that are linked (non-null), excluding a given employee.
     * Used to build "available users" lists.
     */
    @Query("SELECT e.usuario.idUsuario FROM Empleado e WHERE e.usuario IS NOT NULL AND e.id <> :excludeEmpleadoId")
    List<Integer> findLinkedUsuarioIdsExcluding(@Param("excludeEmpleadoId") Long excludeEmpleadoId);

    /**
     * Returns all employee usuario IDs that are linked (non-null).
     * Used when excludeEmpleadoId is null (new employee form).
     */
    @Query("SELECT e.usuario.idUsuario FROM Empleado e WHERE e.usuario IS NOT NULL")
    List<Integer> findAllLinkedUsuarioIds();
}
