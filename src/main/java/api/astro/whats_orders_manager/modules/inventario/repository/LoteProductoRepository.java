package api.astro.whats_orders_manager.modules.inventario.repository;

import api.astro.whats_orders_manager.modules.inventario.enums.EstadoLote;
import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long> {

    List<LoteProducto> findByProductoAndEstadoOrderByFechaVencimientoAsc(
        Producto producto, EstadoLote estado
    );

    List<LoteProducto> findByProducto(Producto producto);

    @Query("SELECT l FROM LoteProducto l " +
           "WHERE l.estado = 'ACTIVO' " +
           "AND l.fechaVencimiento IS NOT NULL " +
           "AND l.fechaVencimiento BETWEEN :desde AND :hasta " +
           "ORDER BY l.fechaVencimiento ASC")
    List<LoteProducto> findLotesPorVencer(
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta
    );

    @Query("SELECT l FROM LoteProducto l " +
           "WHERE l.estado = 'ACTIVO' " +
           "AND l.fechaVencimiento IS NOT NULL " +
           "AND l.fechaVencimiento < :fecha")
    List<LoteProducto> findLotesVencidos(@Param("fecha") LocalDate fecha);
}
