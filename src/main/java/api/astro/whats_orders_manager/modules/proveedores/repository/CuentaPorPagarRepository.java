package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long> {

    List<CuentaPorPagar> findByProveedorOrderByFechaVencimientoAsc(Proveedor proveedor);

    List<CuentaPorPagar> findByEstadoOrderByFechaVencimientoAsc(EstadoCuentaPorPagar estado);

    List<CuentaPorPagar> findByEstadoInOrderByFechaVencimientoAsc(List<EstadoCuentaPorPagar> estados);

    @Query("SELECT c FROM CuentaPorPagar c WHERE c.estado IN ('PENDIENTE', 'PARCIAL') AND c.fechaVencimiento < :hoy ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorPagar> findVencidas(LocalDate hoy);

    @Query("SELECT MAX(CAST(SUBSTRING(c.numero, 10) AS int)) FROM CuentaPorPagar c WHERE c.numero LIKE CONCAT('CPP-', :anio, '-%')")
    Integer findUltimoConsecutivo(int anio);
}
