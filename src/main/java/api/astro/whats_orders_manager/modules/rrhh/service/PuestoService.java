package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.PuestoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;

import java.util.List;

public interface PuestoService {

    Puesto crear(PuestoDTO dto);

    Puesto actualizar(Long id, PuestoDTO dto);

    void desactivar(Long id);

    List<Puesto> findAll();

    List<Puesto> findActivos();

    Puesto findById(Long id);

    /**
     * Returns active puestos belonging to the given departamento.
     * Used for cascade select in empleado form.
     */
    List<Puesto> findByDepartamento(Long departamentoId);
}
