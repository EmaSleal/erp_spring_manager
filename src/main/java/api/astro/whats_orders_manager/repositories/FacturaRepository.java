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
}
