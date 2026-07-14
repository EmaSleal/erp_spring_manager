package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByNumero(String numero);

    List<OrdenCompra> findByProveedorOrderByFechaDesc(Proveedor proveedor);

    List<OrdenCompra> findByEstadoOrderByFechaDesc(EstadoOrdenCompra estado);

    List<OrdenCompra> findByEstadoInOrderByFechaDesc(List<EstadoOrdenCompra> estados);

    @Query("SELECT MAX(CAST(SUBSTRING(o.numero, 9) AS int)) FROM OrdenCompra o WHERE o.numero LIKE CONCAT('OC-', :anio, '-%')")
    Integer findUltimoConsecutivo(int anio);
}
