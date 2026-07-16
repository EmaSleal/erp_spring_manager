package api.astro.whats_orders_manager.modules.inventario.repository;

import api.astro.whats_orders_manager.modules.inventario.enums.TipoMovimientoInventario;
import api.astro.whats_orders_manager.modules.inventario.model.MovimientoInventario;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findAllByOrderByFechaDesc();

    List<MovimientoInventario> findByProductoOrderByFechaDesc(Producto producto);

    List<MovimientoInventario> findByProductoAndFechaBetweenOrderByFechaAsc(
        Producto producto, LocalDateTime desde, LocalDateTime hasta
    );

    List<MovimientoInventario> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    List<MovimientoInventario> findByTipo(TipoMovimientoInventario tipo);

    List<MovimientoInventario> findByDocumentoOrigenAndDocumentoOrigenId(
        String documentoOrigen, Long documentoOrigenId
    );
}
