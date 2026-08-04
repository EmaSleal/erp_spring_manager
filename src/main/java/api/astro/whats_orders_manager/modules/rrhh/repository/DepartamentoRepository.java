package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    Optional<Departamento> findByNombre(String nombre);

    /**
     * Returns true if another departamento with the same nombre exists
     * (excluding the record with the given id, so self-comparison on update is safe).
     * When creating a new record pass id = 0L.
     */
    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<Departamento> findByActivoTrue();

    long countByActivoTrue();
}
