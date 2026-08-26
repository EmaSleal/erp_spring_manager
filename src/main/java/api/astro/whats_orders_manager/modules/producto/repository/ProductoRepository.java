package api.astro.whats_orders_manager.modules.producto.repository;

import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.model.ProductoRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    //call the sp ObtenerProductos()

    @Query(value = "call ObtenerProductos()", nativeQuery = true)
    List<ProductoRecord> findAllRecords();

    // sp to deactivate a product
    @Modifying
    @Query(value = "call sp_desactivar_producto(?1)", nativeQuery = true)
    void desactivarProducto(Integer idProducto);
    
    /**
     * Llama al SP para obtener productos más vendidos
     * @param limite Número de productos a retornar
     * @return Lista de arrays [producto, cantidad_vendida]
     */
    @Query(value = "CALL sp_obtener_productos_mas_vendidos(:limite)", nativeQuery = true)
    List<Object[]> obtenerProductosMasVendidos(@Param("limite") int limite);

    /**
     * Returns all Producto variants linked to the given ArticuloMaestro PK.
     * Used by ArticuloMaestroServiceImpl for write-through, deactivate, reactivate, and delete guard.
     */
    List<Producto> findByArticuloMaestro_IdArticuloMaestro(Integer idArticuloMaestro);

    /**
     * Eager-fetches articuloMaestro to avoid LazyInitializationException
     * when callers read it outside the originating transaction (e.g. report exports).
     */
    @Query("SELECT p FROM Producto p LEFT JOIN FETCH p.articuloMaestro")
    List<Producto> findAllWithArticuloMaestro();

    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockBajo AND p.active = true")
    List<Producto> findProductosStockBajo();

    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo AND p.active = true")
    List<Producto> findProductosStockCritico();

    @Query("SELECT p FROM Producto p WHERE p.stock <= p.puntoReorden AND p.active = true")
    List<Producto> findProductosPuntoReorden();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Integer id);

}