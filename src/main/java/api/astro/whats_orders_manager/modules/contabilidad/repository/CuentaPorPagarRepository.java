package api.astro.whats_orders_manager.modules.contabilidad.repository;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long> {

    List<CuentaPorPagar> findByProveedorOrderByFechaVencimientoAsc(Proveedor proveedor);

    List<CuentaPorPagar> findByEstadoOrderByFechaVencimientoAsc(EstadoCuentaPorPagar estado);

    List<CuentaPorPagar> findByEstadoInOrderByFechaVencimientoAsc(List<EstadoCuentaPorPagar> estados);

    @Query("SELECT c FROM CuentaPorPagar c WHERE c.estado IN ('PENDIENTE', 'PARCIAL') AND c.fechaVencimiento < :hoy ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorPagar> findVencidas(LocalDate hoy);

    @Query("SELECT MAX(CAST(SUBSTRING(c.numero, 10) AS int)) FROM CuentaPorPagar c WHERE c.numero LIKE CONCAT('CPP-', :anio, '-%')")
    Integer findUltimoConsecutivo(int anio);

    boolean existsByOrdenCompra(OrdenCompra ordenCompra);

    /** Id del proveedor de la CxP sin cargar la entidad (para bloquear primero al proveedor, T-052). */
    @Query("SELECT c.proveedor.id FROM CuentaPorPagar c WHERE c.id = :id")
    Optional<Long> findProveedorId(Long id);

    /** La CxP con bloqueo de escritura (SELECT ... FOR UPDATE): estado y saldo leídos tras el bloqueo son los vigentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaPorPagar c WHERE c.id = :id")
    Optional<CuentaPorPagar> findByIdForUpdate(Long id);

    /** ¿El proveedor ya tiene una CxP no anulada con ese comprobante normalizado? (T-052) */
    boolean existsByProveedorAndComprobanteNormalizado(Proveedor proveedor, String comprobanteNormalizado);

    /** Las CxP del proveedor que cuentan para su saldo: todas menos las del estado indicado (ANULADA). */
    List<CuentaPorPagar> findByProveedorAndEstadoNot(Proveedor proveedor, EstadoCuentaPorPagar estado);

    /** Listado con proveedor y orden de compra ya cargados, para armar DTO sin cargas diferidas (T-052). */
    @Query("SELECT c FROM CuentaPorPagar c JOIN FETCH c.proveedor LEFT JOIN FETCH c.ordenCompra ORDER BY c.id DESC")
    List<CuentaPorPagar> findAllConRelaciones();

    @Query("SELECT c FROM CuentaPorPagar c JOIN FETCH c.proveedor LEFT JOIN FETCH c.ordenCompra "
         + "WHERE c.estado = :estado ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorPagar> findByEstadoConRelaciones(EstadoCuentaPorPagar estado);

    @Query("SELECT c FROM CuentaPorPagar c JOIN FETCH c.proveedor LEFT JOIN FETCH c.ordenCompra "
         + "WHERE c.estado IN ('PENDIENTE', 'PARCIAL') AND c.fechaVencimiento < :hoy ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorPagar> findVencidasConRelaciones(LocalDate hoy);

    @Query("SELECT c.ordenCompra.id FROM CuentaPorPagar c WHERE c.ordenCompra IS NOT NULL")
    List<Long> findOrdenCompraIds();
}
