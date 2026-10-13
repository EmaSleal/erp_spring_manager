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

    // Latest active rate on or before a given date (ADR-011: rate in force for that day)
    Optional<TipoCambio> findFirstByMonedaDestinoAndActivoTrueAndFechaLessThanEqualOrderByFechaDescIdDesc(
            MonedaFE monedaDestino, LocalDate fecha);

    // Exact date lookup for conflict detection on registrar
    Optional<TipoCambio> findByFechaAndMonedaDestinoAndActivoTrue(LocalDate fecha, MonedaFE monedaDestino);

    // History for a currency, most recent first (caller limits to 30)
    List<TipoCambio> findByMonedaDestinoOrderByFechaDesc(MonedaFE monedaDestino);

    // Atomically deactivate any existing active row for the same date+currency.
    // Bulk UPDATE: runs in the database immediately, before the caller's INSERT of the new active row
    // (V26 allows a single active row per date + currency pair); pending changes are flushed first.
    @Modifying(flushAutomatically = true)
    @Query("UPDATE TipoCambio t SET t.activo = false WHERE t.fecha = :fecha AND t.monedaDestino = :monedaDestino AND t.activo = true")
    void deactivateExisting(@Param("fecha") LocalDate fecha, @Param("monedaDestino") MonedaFE monedaDestino);
}
