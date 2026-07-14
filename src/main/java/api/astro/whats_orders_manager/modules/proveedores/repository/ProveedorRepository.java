package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByCedula(String cedula);

    List<Proveedor> findByActivoTrueOrderByNombreAsc();

    List<Proveedor> findByTipoProveedorAndActivoTrue(TipoProveedor tipo);

    boolean existsByCedula(String cedula);

    @Query("SELECT p FROM Proveedor p WHERE p.activo = true " +
           "AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(p.cedula) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Proveedor> buscar(String q);
}
