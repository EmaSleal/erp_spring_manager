package api.astro.whats_orders_manager.modules.nomina.repository;

import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NominaRepository extends JpaRepository<Nomina, Long> {

    /**
     * Duplicate-period guard: returns true when a payroll for the same
     * (periodoInicio, periodoFin, tipo) already exists regardless of estado.
     */
    boolean existsByPeriodoInicioAndPeriodoFinAndTipo(
            java.time.LocalDate periodoInicio,
            java.time.LocalDate periodoFin,
            TipoNomina tipo);
}
