package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;

import java.util.List;

public interface TramoImpuestoSalarioService {

    /**
     * Returns all tax brackets for the given year, ordered by limiteInferior ASC.
     */
    List<TramoImpuestoSalario> findByAnioVigencia(int anioVigencia);

    List<TramoImpuestoSalario> findAll();
}
