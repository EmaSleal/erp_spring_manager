package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByCedula(String cedula);

    /**
     * El proveedor con bloqueo de escritura (SELECT ... FOR UPDATE). Serializa por proveedor el alta y la anulación de
     * cuentas por pagar: la comprobación de comprobante duplicado y el recálculo del saldo no se pisan (T-052).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Proveedor p WHERE p.id = :id")
    Optional<Proveedor> findByIdForUpdate(Long id);

    List<Proveedor> findByActivoTrueOrderByNombreAsc();

    List<Proveedor> findByTipoProveedorAndActivoTrue(TipoProveedor tipo);

    boolean existsByCedula(String cedula);

    @Query("SELECT p FROM Proveedor p WHERE p.activo = true " +
           "AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(p.cedula) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Proveedor> buscar(String q);
}
