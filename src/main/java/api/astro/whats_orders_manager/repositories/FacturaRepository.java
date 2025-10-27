package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    
    /**
     * Cuenta las facturas creadas hoy
     * Usa createDate para contar facturas del día actual
     */
    @Query("SELECT COUNT(f) FROM Factura f WHERE CAST(f.createDate AS date) = CURRENT_DATE")
    long countByFechaToday();
    
    /**
     * Suma el total de facturas no entregadas (pendientes)
     */
    @Query("SELECT COALESCE(SUM(f.total), 0) FROM Factura f WHERE f.entregado = false")
    BigDecimal sumTotalPendiente();
    
    /**
     * Busca una factura por su número único
     * @param numeroFactura Número de factura (ej: "F001-00001")
     * @return Optional con la factura si existe
     */
    @Query("SELECT f FROM Factura f WHERE f.numeroFactura = :numeroFactura")
    Optional<Factura> findByNumeroFactura(@Param("numeroFactura") String numeroFactura);
    
    /**
     * Verifica si existe una factura con el número especificado
     * @param numeroFactura Número de factura a verificar
     * @return true si existe
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Factura f WHERE f.numeroFactura = :numeroFactura")
    boolean existsByNumeroFactura(@Param("numeroFactura") String numeroFactura);
    
    /**
     * Busca facturas con pago vencido para enviar recordatorios
     * Criterios: fechaPago < hoy, entregado = true, cliente con email
     * @return Lista de facturas con pago vencido
     */
    @Query("SELECT f FROM Factura f " +
           "WHERE f.fechaPago < CURRENT_DATE " +
           "AND f.entregado = true " +
           "AND f.cliente.email IS NOT NULL")
    List<Factura> findFacturasConPagoVencido();
    
    /**
     * Llama al SP para obtener ventas por mes
     * @param meses Número de meses hacia atrás
     * @return Lista de arrays [mes, total_ventas]
     */
    @Query(value = "CALL sp_obtener_ventas_por_mes(:meses)", nativeQuery = true)
    List<Object[]> obtenerVentasPorMes(@Param("meses") int meses);
    
    /**
     * Llama al SP para obtener ventas por día en un rango
     * @param fechaInicio Fecha de inicio (puede ser null)
     * @param fechaFin Fecha de fin (puede ser null)
     * @param clienteId ID del cliente (puede ser null)
     * @return Lista de arrays [fecha, total_ventas]
     */
    @Query(value = "CALL sp_obtener_ventas_por_dia(:fechaInicio, :fechaFin, :clienteId)", nativeQuery = true)
    List<Object[]> obtenerVentasPorDia(
        @Param("fechaInicio") java.sql.Date fechaInicio,
        @Param("fechaFin") java.sql.Date fechaFin,
        @Param("clienteId") Integer clienteId
    );
    
    /**
     * Llama al SP para obtener estadísticas de ventas
     * @param fechaInicio Fecha de inicio (puede ser null)
     * @param fechaFin Fecha de fin (puede ser null)
     * @return Array con estadísticas [total_facturas, total_ventas, ticket_promedio, etc.]
     */
    @Query(value = "CALL sp_obtener_estadisticas_ventas(:fechaInicio, :fechaFin)", nativeQuery = true)
    Object[] obtenerEstadisticasVentas(
        @Param("fechaInicio") java.sql.Date fechaInicio,
        @Param("fechaFin") java.sql.Date fechaFin
    );
}
