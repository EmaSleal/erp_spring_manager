package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.ContratoEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoEmpleadoRepository extends JpaRepository<ContratoEmpleado, Long> {

    List<ContratoEmpleado> findByEmpleadoId(Long empleadoId);

    Optional<ContratoEmpleado> findByEmpleadoIdAndActivoTrue(Long empleadoId);

    long countByActivoTrue();
}
