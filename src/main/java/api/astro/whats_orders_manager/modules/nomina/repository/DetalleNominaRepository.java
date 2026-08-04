package api.astro.whats_orders_manager.modules.nomina.repository;

import api.astro.whats_orders_manager.modules.nomina.model.DetalleNomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleNominaRepository extends JpaRepository<DetalleNomina, Long> {

    /** Returns all detail lines for a given payroll run. */
    List<DetalleNomina> findByNominaId(Long nominaId);
}
