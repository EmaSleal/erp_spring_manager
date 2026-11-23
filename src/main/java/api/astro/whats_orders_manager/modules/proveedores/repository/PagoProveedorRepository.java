package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {

    List<PagoProveedor> findByCuentaPorPagarOrderByFechaDesc(CuentaPorPagar cuenta);

    boolean existsByCuentaPorPagarAndEstado(CuentaPorPagar cuenta, EstadoPagoProveedor estado);

    /**
     * Pagos de la CxP en ese estado con lectura bloqueante: ve lo último confirmado aunque la transacción ya tenga
     * una instantánea de lectura anterior al bloqueo de la CxP (REPEATABLE READ). Lo usa la anulación (T-052).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoProveedor p WHERE p.cuentaPorPagar = :cuenta AND p.estado = :estado")
    List<PagoProveedor> findByCuentaPorPagarAndEstadoForUpdate(CuentaPorPagar cuenta, EstadoPagoProveedor estado);

    /** Id de la CxP del pago sin cargar el pago (para bloquear antes de leer su estado, T-053). */
    @Query("SELECT p.cuentaPorPagar.id FROM PagoProveedor p WHERE p.id = :id")
    Optional<Long> findCuentaPorPagarId(Long id);

    /** El pago con bloqueo de escritura (SELECT ... FOR UPDATE): el estado leído tras el bloqueo es el vigente (T-053). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoProveedor p WHERE p.id = :id")
    Optional<PagoProveedor> findByIdForUpdate(Long id);

    List<PagoProveedor> findByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta);

    @Query("SELECT MAX(CAST(SUBSTRING(p.numero, 9) AS int)) FROM PagoProveedor p WHERE p.numero LIKE CONCAT('PP-', :anio, '-%')")
    Integer findUltimoConsecutivo(int anio);
}
