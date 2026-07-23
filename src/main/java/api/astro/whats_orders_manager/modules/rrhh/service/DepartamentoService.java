package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.DepartamentoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;

import java.util.List;

public interface DepartamentoService {

    Departamento crear(DepartamentoDTO dto);

    Departamento actualizar(Long id, DepartamentoDTO dto);

    /**
     * Soft-delete. Throws {@link IllegalStateException} if the department has
     * at least one active Puesto.
     */
    void desactivar(Long id);

    List<Departamento> findAll();

    List<Departamento> findActivos();

    Departamento findById(Long id);
}
