package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCambioRepository extends JpaRepository<TipoCambio, Long> {

    // Latest active rate for a destination currency
    Optional<TipoCambio> findFirstByMonedaDestinoAndActivoTrueOrderByFechaDesc(MonedaFE monedaDestino);

    // Exact date lookup for conflict detection on registrar
    Optional<TipoCambio> findByFechaAndMonedaDestinoAndActivoTrue(LocalDate fecha, MonedaFE monedaDestino);

    // History for a currency, most recent first (caller limits to 30)
    List<TipoCambio> findByMonedaDestinoOrderByFechaDesc(MonedaFE monedaDestino);

    // Atomically deactivate any existing active row for the same date+currency
    @Modifying
    @Query("UPDATE TipoCambio t SET t.activo = false WHERE t.fecha = :fecha AND t.monedaDestino = :monedaDestino AND t.activo = true")
    void deactivateExisting(@Param("fecha") LocalDate fecha, @Param("monedaDestino") MonedaFE monedaDestino);
}
