package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TramoImpuestoSalarioRepository extends JpaRepository<TramoImpuestoSalario, Long> {

    /**
     * Returns all tax brackets for the given year, ordered by limiteInferior ASC.
     * The first element has limiteInferior=0 (exempt bracket) and contains tax credits.
     */
    List<TramoImpuestoSalario> findByAnioVigenciaOrderByLimiteInferiorAsc(int anioVigencia);
}
