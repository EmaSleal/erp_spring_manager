package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByNumero(String numero);

    /** Id del proveedor de la orden sin cargar la entidad (para bloquear primero al proveedor, T-053). */
    @Query("SELECT o.proveedor.id FROM OrdenCompra o WHERE o.id = :id")
    Optional<Long> findProveedorId(Long id);

    /** La orden con bloqueo de escritura (SELECT ... FOR UPDATE): estado y total leídos tras el bloqueo son los vigentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrdenCompra o WHERE o.id = :id")
    Optional<OrdenCompra> findByIdForUpdate(Long id);

    List<OrdenCompra> findByProveedorOrderByFechaDesc(Proveedor proveedor);

    List<OrdenCompra> findByEstadoOrderByFechaDesc(EstadoOrdenCompra estado);

    List<OrdenCompra> findByEstadoInOrderByFechaDesc(List<EstadoOrdenCompra> estados);

    @Query("SELECT MAX(CAST(SUBSTRING(o.numero, 9) AS int)) FROM OrdenCompra o WHERE o.numero LIKE CONCAT('OC-', :anio, '-%')")
    Integer findUltimoConsecutivo(int anio);
}
