package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    
    /**
     * Cuenta las facturas creadas hoy usando un rango explícito de LocalDateTime.
     * Callers must pass: startOfDay = LocalDate.now().atStartOfDay(),
     *                    startOfTomorrow = LocalDate.now().plusDays(1).atStartOfDay()
     * Avoids CAST(... AS date) which is dialect-specific and fails at midnight boundaries.
     */
    @Query("SELECT COUNT(f) FROM Factura f WHERE f.createDate >= :startOfDay AND f.createDate < :startOfTomorrow")
    long countByFechaToday(@Param("startOfDay") LocalDateTime startOfDay,
                           @Param("startOfTomorrow") LocalDateTime startOfTomorrow);
    
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
    
    /**
     * Busca todas las facturas de un cliente específico
     * @param idCliente ID del cliente
     * @return Lista de facturas del cliente
     */
    List<Factura> findByClienteIdCliente(Integer idCliente);


    //findById
    Optional<Factura> findByIdFactura(Integer idFactura);

    //findByClienteId(idCliente)
    @Query("SELECT f FROM Factura f WHERE f.cliente.idCliente = :idCliente")
    Optional<List<Factura>> findByClienteId(@Param("idCliente") Integer idCliente);

    /**
     * Busca facturas paginadas aplicando filtros opcionales del listado.
     * Cada filtro se ignora cuando su parámetro es null.
     * sinFE y estadoFE son mutuamente excluyentes: sinFE busca facturas sin
     * comprobante electrónico, mientras que estadoFE filtra por el estado del comprobante.
     */
    @Query(value = "SELECT f FROM Factura f LEFT JOIN f.comprobanteElectronico c " +
           "WHERE (:startDate IS NULL OR CAST(f.fechaEntrega AS date) >= :startDate) " +
           "AND (:endDate IS NULL OR CAST(f.fechaEntrega AS date) <= :endDate) " +
           "AND (:entregado IS NULL OR f.entregado = :entregado) " +
           "AND (:pagada IS NULL OR (:pagada = true AND f.estadoPago = 'PAGADO_TOTAL') OR (:pagada = false AND f.estadoPago <> 'PAGADO_TOTAL')) " +
           "AND (:sinFE IS NULL OR c IS NULL) " +
           "AND (:estadoFE IS NULL OR c.estado = :estadoFE)",
           countQuery = "SELECT COUNT(f) FROM Factura f LEFT JOIN f.comprobanteElectronico c " +
           "WHERE (:startDate IS NULL OR CAST(f.fechaEntrega AS date) >= :startDate) " +
           "AND (:endDate IS NULL OR CAST(f.fechaEntrega AS date) <= :endDate) " +
           "AND (:entregado IS NULL OR f.entregado = :entregado) " +
           "AND (:pagada IS NULL OR (:pagada = true AND f.estadoPago = 'PAGADO_TOTAL') OR (:pagada = false AND f.estadoPago <> 'PAGADO_TOTAL')) " +
           "AND (:sinFE IS NULL OR c IS NULL) " +
           "AND (:estadoFE IS NULL OR c.estado = :estadoFE)")
    Page<Factura> buscarConFiltros(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("entregado") Boolean entregado,
            @Param("pagada") Boolean pagada,
            @Param("sinFE") Boolean sinFE,
            @Param("estadoFE") EstadoComprobante estadoFE,
            Pageable pageable
    );
}
