package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroCCSS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ParametroCCSSRepository extends JpaRepository<ParametroCCSS, Long> {

    /**
     * Returns the ParametroCCSS record active on the given date.
     * Condition: vigenciaDesde <= fecha AND (vigenciaHasta IS NULL OR vigenciaHasta >= fecha)
     */
    @Query("""
        SELECT p FROM ParametroCCSS p
        WHERE p.vigenciaDesde <= :fecha
          AND (p.vigenciaHasta IS NULL OR p.vigenciaHasta >= :fecha)
        ORDER BY p.vigenciaDesde DESC
        """)
    Optional<ParametroCCSS> findVigenteByFecha(@Param("fecha") LocalDate fecha);
}
