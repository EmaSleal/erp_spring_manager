package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.AusenciaDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;

import java.util.List;

public interface AusenciaService {

    /**
     * Registers a new absence for the given employee.
     * Rejects the request if the date range overlaps any already-approved
     * absence for the same employee.
     */
    Ausencia registrar(AusenciaDTO dto);

    /**
     * Approves the absence identified by id.
     * Sets aprobada=true and aprobadaPor to the usuario with usuarioAprobadorId.
     */
    void aprobar(Long id, Integer usuarioAprobadorId);

    /**
     * Returns all absences for the given employee.
     */
    List<Ausencia> findByEmpleado(Long empleadoId);

    /**
     * Returns all absences across all employees.
     */
    List<Ausencia> findAll();
}
