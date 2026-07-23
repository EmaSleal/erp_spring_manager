package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PuestoRepository extends JpaRepository<Puesto, Long> {

    List<Puesto> findByDepartamentoIdAndActivoTrue(Long departamentoId);

    /**
     * Used by DepartamentoService.desactivar() to guard against deactivating
     * a department that still has active puestos.
     */
    boolean existsByActivoTrueAndDepartamento(Departamento departamento);

    long countByActivoTrue();
}
