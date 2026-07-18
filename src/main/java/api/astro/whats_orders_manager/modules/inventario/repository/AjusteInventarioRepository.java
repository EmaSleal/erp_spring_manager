package api.astro.whats_orders_manager.modules.inventario.repository;

import api.astro.whats_orders_manager.modules.inventario.enums.EstadoAjuste;
import api.astro.whats_orders_manager.modules.inventario.model.AjusteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long> {

    Optional<AjusteInventario> findByNumero(String numero);

    List<AjusteInventario> findByEstadoOrderByFechaDesc(EstadoAjuste estado);

    List<AjusteInventario> findByFechaBetweenOrderByFechaDesc(
        LocalDateTime desde, LocalDateTime hasta
    );

    @Query("SELECT MAX(CAST(SUBSTRING(a.numero, 9) AS int)) FROM AjusteInventario a " +
           "WHERE a.numero LIKE CONCAT('AJ-', :anio, '-%')")
    Integer findUltimoConsecutivo(@Param("anio") int anio);
}
