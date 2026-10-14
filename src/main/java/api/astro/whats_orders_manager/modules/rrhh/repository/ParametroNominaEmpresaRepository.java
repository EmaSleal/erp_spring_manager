package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroNominaEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ParametroNominaEmpresaRepository extends JpaRepository<ParametroNominaEmpresa, Long> {

    /**
     * Devuelve el registro de ParametroNominaEmpresa vigente en la fecha dada.
     * Condición: vigenciaDesde <= fecha AND (vigenciaHasta IS NULL OR vigenciaHasta >= fecha)
     */
    @Query("""
        SELECT p FROM ParametroNominaEmpresa p
        WHERE p.vigenciaDesde <= :fecha
          AND (p.vigenciaHasta IS NULL OR p.vigenciaHasta >= :fecha)
        ORDER BY p.vigenciaDesde DESC
        """)
    Optional<ParametroNominaEmpresa> findVigenteByFecha(@Param("fecha") LocalDate fecha);
}
