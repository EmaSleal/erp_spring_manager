package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.LineaFactura;
import api.astro.whats_orders_manager.models.LineaFacturaR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineaFacturaRepository extends JpaRepository<LineaFactura, Integer> {

    //sp to get the lineas of a factura
    @Query(value = "call sp_get_lineas_factura(:idFactura)", nativeQuery = true)
    List<LineaFacturaR> findLineasByFacturaId(Integer idFactura);

//    CREATE PROCEDURE sp_actualizar_linea_factura (
//            IN p_id_linea_factura INT,
//            IN p_numero_linea INT,
//            IN p_id_producto INT,
//            IN p_cantidad INT,
//            IN p_precio_unitario DECIMAL(10,2),
//    IN p_subtotal DECIMAL(10,2),
//    IN p_update_by INT
//)
    //sp to update lineas of a factura
@Modifying
@Query(value = "call sp_actualizar_linea_factura(:id_linea_factura, :numero_linea, :id_factura, :id_producto, :cantidad, :precio_unitario, :subtotal, :update_by)", nativeQuery = true)
void updateLinea(
        @Param("id_linea_factura") Integer idLineaFactura,
        @Param("numero_linea") Integer numeroLinea,
        @Param("id_producto") Integer idProducto,
        @Param("cantidad") Integer cantidad,
        @Param("precio_unitario") Double precioUnitario,
        @Param("subtotal") Double subtotal,
        @Param("update_by") Integer updateBy,
        @Param("id_factura") Integer idFactura
);
@Query(value = "SELECT EXISTS(SELECT TRUE FROM linea_factura WHERE id_factura = :id)", nativeQuery = true)
long existsByFacturaId(Integer id);

}
