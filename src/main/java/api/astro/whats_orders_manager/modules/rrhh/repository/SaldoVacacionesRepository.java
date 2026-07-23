package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.SaldoVacaciones;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaldoVacacionesRepository extends JpaRepository<SaldoVacaciones, Long> {

    Optional<SaldoVacaciones> findByEmpleadoId(Long empleadoId);
}
