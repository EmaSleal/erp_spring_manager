package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.proveedores.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {

    List<PagoProveedor> findByCuentaPorPagarOrderByFechaDesc(CuentaPorPagar cuenta);

    List<PagoProveedor> findByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta);

    @Query("SELECT MAX(CAST(SUBSTRING(p.numero, 9) AS int)) FROM PagoProveedor p WHERE p.numero LIKE CONCAT('PP-', :anio, '-%')")
    Integer findUltimoConsecutivo(int anio);
}
