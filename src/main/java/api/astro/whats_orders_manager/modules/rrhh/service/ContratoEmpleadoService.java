package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.ContratoEmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.CausaTerminacion;
import api.astro.whats_orders_manager.modules.rrhh.model.ContratoEmpleado;

import java.time.LocalDate;
import java.util.List;

public interface ContratoEmpleadoService {

    /**
     * Creates a new contract for the given employee.
     * Deactivates the current active contract (if any) before persisting the new one.
     * Both operations run in the same transaction.
     */
    ContratoEmpleado crear(ContratoEmpleadoDTO dto);

    /**
     * Terminates the contract identified by id.
     * Sets activo=false, causaTerminacion, fechaTerminacion, and descripcionTerminacion.
     */
    void terminar(Long id, CausaTerminacion causa, LocalDate fechaTerminacion, String descripcion);

    /**
     * Returns all contracts for the given employee (active and inactive).
     */
    List<ContratoEmpleado> findByEmpleado(Long empleadoId);

    /**
     * Returns all contracts across all employees.
     */
    List<ContratoEmpleado> findAll();
}
