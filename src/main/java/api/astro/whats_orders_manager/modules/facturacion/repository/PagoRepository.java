package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de pagos aplicados a facturas.
 * Soporta consultas por factura, método de pago, estado y rangos de fechas.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    
    /**
     * Encuentra todos los pagos de una factura específica.
     * @param factura Factura a consultar
     * @return Lista de pagos de la factura
     */
    @Query("SELECT p FROM Pago p WHERE p.factura = :factura ORDER BY p.fechaPago DESC")
    List<Pago> findByFactura(@Param("factura") Factura factura);
    
    /**
     * Encuentra todos los pagos de una factura por su ID.
     * @param idFactura ID de la factura
     * @return Lista de pagos de la factura
     */
    @Query("SELECT p FROM Pago p WHERE p.factura.idFactura = :idFactura ORDER BY p.fechaPago DESC")
    List<Pago> findByFacturaId(@Param("idFactura") Integer idFactura);
    
    /**
     * Encuentra pagos válidos (CONFIRMADO o CONCILIADO) de una factura.
     * @param idFactura ID de la factura
     * @return Lista de pagos válidos
     */
    @Query("SELECT p FROM Pago p WHERE p.factura.idFactura = :idFactura " +
           "AND p.estado IN ('CONFIRMADO', 'CONCILIADO') " +
           "ORDER BY p.fechaPago DESC")
    List<Pago> findPagosValidosByFacturaId(@Param("idFactura") Integer idFactura);
    
    /**
     * Calcula el total de pagos válidos de una factura.
     * @param idFactura ID de la factura
     * @return Total pagado (0 si no hay pagos)
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
           "WHERE p.factura.idFactura = :idFactura " +
           "AND p.estado IN ('CONFIRMADO', 'CONCILIADO')")
    BigDecimal calcularTotalPagadoPorFactura(@Param("idFactura") Integer idFactura);
    
    /**
     * Encuentra pagos por método de pago.
     * @param metodoPago Método de pago a filtrar
     * @return Lista de pagos con el método especificado
     */
    @Query("SELECT p FROM Pago p WHERE p.metodoPago = :metodoPago ORDER BY p.fechaPago DESC")
    List<Pago> findByMetodoPago(@Param("metodoPago") MetodoPago metodoPago);
    
    /**
     * Encuentra pagos por estado.
     * @param estado Estado del pago
     * @return Lista de pagos con el estado especificado
     */
    @Query("SELECT p FROM Pago p WHERE p.estado = :estado ORDER BY p.fechaPago DESC")
    List<Pago> findByEstado(@Param("estado") EstadoPago estado);
    
    /**
     * Encuentra pagos pendientes de conciliación.
     * @return Lista de pagos confirmados pero no conciliados
     */
    @Query("SELECT p FROM Pago p WHERE p.estado = 'CONFIRMADO' AND p.conciliado = false ORDER BY p.fechaPago")
    List<Pago> findPagosPendientesConciliacion();
    
    /**
     * Encuentra pagos en un rango de fechas.
     * @param fechaInicio Fecha inicial del rango
     * @param fechaFin Fecha final del rango
     * @return Lista de pagos en el rango
     */
    @Query("SELECT p FROM Pago p WHERE p.fechaPago BETWEEN :fechaInicio AND :fechaFin ORDER BY p.fechaPago DESC")
    List<Pago> findByFechaPagoBetween(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Cuenta pagos por método de pago en un rango de fechas.
     * Útil para reportes de caja.
     * @param metodoPago Método de pago
     * @param fechaInicio Fecha inicial
     * @param fechaFin Fecha final
     * @return Cantidad de pagos
     */
    @Query("SELECT COUNT(p) FROM Pago p WHERE p.metodoPago = :metodoPago " +
           "AND p.fechaPago BETWEEN :fechaInicio AND :fechaFin " +
           "AND p.estado IN ('CONFIRMADO', 'CONCILIADO')")
    long countByMetodoPagoAndFecha(
        @Param("metodoPago") MetodoPago metodoPago,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Suma total de pagos por método de pago en un rango de fechas.
     * Útil para reportes de caja y cuadre diario.
     * @param metodoPago Método de pago
     * @param fechaInicio Fecha inicial
     * @param fechaFin Fecha final
     * @return Total de pagos
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.metodoPago = :metodoPago " +
           "AND p.fechaPago BETWEEN :fechaInicio AND :fechaFin " +
           "AND p.estado IN ('CONFIRMADO', 'CONCILIADO')")
    BigDecimal sumByMetodoPagoAndFecha(
        @Param("metodoPago") MetodoPago metodoPago,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Encuentra pagos por referencia (número de cheque, transacción, etc.).
     * @param referenciaBancaria Referencia a buscar
     * @return Pago con la referencia especificada
     */
    @Query("SELECT p FROM Pago p WHERE p.referenciaBancaria = :referenciaBancaria")
    Optional<Pago> findByReferenciaBancaria(@Param("referenciaBancaria") String referenciaBancaria);
    
    /**
     * Verifica si existe un pago con la referencia especificada.
     * Útil para evitar duplicados en cheques o transferencias.
     * @param referenciaBancaria Referencia a verificar
     * @return true si existe
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Pago p WHERE p.referenciaBancaria = :referenciaBancaria")
    boolean existsByReferenciaBancaria(@Param("referenciaBancaria") String referenciaBancaria);
    
    /**
     * Obtiene el total de ingresos del día actual.
     * Solo considera pagos confirmados y conciliados.
     * @return Total de ingresos del día
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
           "WHERE CAST(p.fechaPago AS date) = CURRENT_DATE " +
           "AND p.estado IN ('CONFIRMADO', 'CONCILIADO')")
    BigDecimal sumIngresosDiaActual();
    
    /**
     * Obtiene pagos creados hoy para reporte de caja diaria.
     * @return Lista de pagos del día
     */
    @Query("SELECT p FROM Pago p WHERE CAST(p.fechaPago AS date) = CURRENT_DATE ORDER BY p.fechaPago")
    List<Pago> findPagosDelDia();
    
    /**
     * Cuenta pagos creados hoy.
     * @return Cantidad de pagos del día
     */
    @Query("SELECT COUNT(p) FROM Pago p WHERE CAST(p.fechaPago AS date) = CURRENT_DATE")
    long countPagosDelDia();
    
    /**
     * Finds all payments for a given client, ordered by payment date descending.
     * Replaces the in-memory stream filter in PagoServiceImpl.findByClienteId.
     *
     * @param idCliente client primary key
     * @return payments belonging to the client
     */
    @Query("SELECT p FROM Pago p WHERE p.cliente.idCliente = :idCliente ORDER BY p.fechaPago DESC")
    List<Pago> findByClienteId(@Param("idCliente") Integer idCliente);

    /**
     * Busca el último número de pago que coincida con un patrón.
     * Útil para generar números consecutivos.
     * @param patron Patrón SQL LIKE (ej: "PAG-20260119-%")
     * @return Último número de pago o null si no existe
     */
    @Query(value = "SELECT numero_pago FROM pagos WHERE numero_pago LIKE :patron ORDER BY numero_pago DESC LIMIT 1", 
           nativeQuery = true)
    String findUltimoNumeroPagoPorPatron(@Param("patron") String patron);
    
    /**
     * Busca un pago por su número único.
     * @param numeroPago Número de pago a buscar
     * @return Pago encontrado o vacío
     */
    @Query("SELECT p FROM Pago p WHERE p.numeroPago = :numeroPago")
    Optional<Pago> findByNumeroPago(@Param("numeroPago") String numeroPago);
    
    /**
     * Verifica si existe un pago con el número especificado.
     * @param numeroPago Número a verificar
     * @return true si existe
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Pago p WHERE p.numeroPago = :numeroPago")
    boolean existsByNumeroPago(@Param("numeroPago") String numeroPago);
    
    /**
     * Returns grouped payment totals by MetodoPago for a date range and estado filter.
     * Column order in Object[]: [0] MetodoPago, [1] BigDecimal total (SUM), [2] Long count (COUNT)
     *
     * @param inicio start of date range (inclusive)
     * @param fin    end of date range (inclusive)
     * @param estados list of valid estados to include
     * @return list of Object[] rows with aggregated data
     */
    @Query("SELECT p.metodoPago, SUM(p.monto), COUNT(p) FROM Pago p " +
           "WHERE p.fechaPago BETWEEN :inicio AND :fin AND p.estado IN :estados " +
           "GROUP BY p.metodoPago")
    List<Object[]> findTotalesByMetodoPagoAndFecha(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estados") List<EstadoPago> estados);

    /**
     * Obtiene todos los pagos como DTOs con paginación.
     * Proyección optimizada para listar pagos sin cargar entidades completas.
     * 
     * @param pageable Configuración de paginación
     * @return Página de DTOs con información de pagos
     */
    @Query("""
        SELECT new api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO(
            p.idPago,
            p.numeroPago,
            p.monto,
            p.metodoPago,
            p.estado,
            p.tipoPago,
            p.fechaPago,
            p.referenciaBancaria,
            p.observaciones,
            p.conciliado,
            p.fechaConciliacion,
            p.cliente.idCliente,
            p.cliente.nombre,
            p.factura.idFactura,
            p.factura.numeroFactura,
            p.factura.total,
            CASE WHEN p.factura IS NOT NULL 
                THEN (p.factura.total - COALESCE((SELECT SUM(pg.monto) FROM Pago pg 
                    WHERE pg.factura.idFactura = p.factura.idFactura 
                    AND pg.estado IN ('CONFIRMADO', 'CONCILIADO')), 0)) 
                ELSE NULL 
            END
        )
        FROM Pago p
        LEFT JOIN p.cliente
        LEFT JOIN p.factura
        ORDER BY p.fechaPago DESC
    """)
    Page<PagoDTO> findAllAsDTO(Pageable pageable);
}

