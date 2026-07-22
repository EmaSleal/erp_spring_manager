package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoAusencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AusenciaRepository extends JpaRepository<Ausencia, Long> {

    List<Ausencia> findByEmpleadoId(Long empleadoId);

    List<Ausencia> findByEmpleadoIdAndTipoAusencia(Long empleadoId, TipoAusencia tipoAusencia);

    long countByAprobadaFalse();

    /**
     * Returns all pending (aprobada=false) absences for employees whose department
     * is managed by the given jefe (department head).
     *
     * Used by PerfilController to display the self-service approval widget.
     */
    @Query("SELECT a FROM Ausencia a JOIN FETCH a.empleado e JOIN e.departamento d WHERE d.jefe.id = :jefeId AND a.aprobada = false")
    List<Ausencia> findPendientesByJefeId(@Param("jefeId") Long jefeId);
}
