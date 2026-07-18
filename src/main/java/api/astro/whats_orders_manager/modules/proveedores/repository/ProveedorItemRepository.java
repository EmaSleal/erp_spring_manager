package api.astro.whats_orders_manager.modules.proveedores.repository;

import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.ProveedorItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorItemRepository extends JpaRepository<ProveedorItem, Long> {
    List<ProveedorItem> findByProveedorIdAndTipo(Long proveedorId, CategoriaProveedor tipo);
    List<ProveedorItem> findByTipo(CategoriaProveedor tipo);
    void deleteByProveedorIdAndTipo(Long proveedorId, CategoriaProveedor tipo);
    void deleteByProveedorId(Long proveedorId);
}
