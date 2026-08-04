package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.SalarioMinimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalarioMinimoRepository extends JpaRepository<SalarioMinimo, Long> {

    /**
     * Returns the vigente SalarioMinimo for a given categoria and date.
     * Condition: vigenciaDesde <= fecha AND (vigenciaHasta IS NULL OR vigenciaHasta >= fecha)
     */
    @Query("""
        SELECT s FROM SalarioMinimo s
        WHERE s.categoria = :categoria
          AND s.vigenciaDesde <= :fecha
          AND (s.vigenciaHasta IS NULL OR s.vigenciaHasta >= :fecha)
        ORDER BY s.vigenciaDesde DESC
        """)
    Optional<SalarioMinimo> findVigenteByCategoria(
            @Param("categoria") String categoria,
            @Param("fecha") LocalDate fecha);

    List<SalarioMinimo> findByCategoria(String categoria);
}
