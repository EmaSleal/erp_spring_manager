package api.astro.whats_orders_manager.modules.contabilidad.repository;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad AsientoContable.
 * Gestiona los asientos contables del sistema de doble partida.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Repository
public interface AsientoContableRepository extends JpaRepository<AsientoContable, Long> {
    
    /**
     * Busca un asiento por su número único.
     * @param numero Número del asiento
     * @return Asiento si existe
     */
    Optional<AsientoContable> findByNumero(String numero);
    
    /**
     * Verifica si existe un asiento con el número dado.
     * @param numero Número a verificar
     * @return true si existe
     */
    boolean existsByNumero(String numero);
    
    /**
     * Busca asientos por fecha.
     * @param fecha Fecha del asiento
     * @return Lista de asientos en la fecha
     */
    List<AsientoContable> findByFecha(LocalDate fecha);
    
    /**
     * Busca asientos en un rango de fechas.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de asientos en el rango
     */
    @Query("SELECT a FROM AsientoContable a WHERE a.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY a.fecha, a.numero")
    List<AsientoContable> findByFechaBetween(@Param("fechaInicio") LocalDate fechaInicio, 
                                              @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Busca asientos por tipo.
     * @param tipo Tipo de asiento
     * @return Lista de asientos del tipo
     */
    List<AsientoContable> findByTipo(TipoAsiento tipo);
    
    /**
     * Busca asientos por estado.
     * @param estado Estado del asiento
     * @return Lista de asientos en ese estado
     */
    List<AsientoContable> findByEstado(EstadoAsiento estado);
    
    /**
     * Busca asientos por estado con paginación.
     * @param estado Estado del asiento
     * @param pageable Configuración de paginación
     * @return Página de asientos
     */
    Page<AsientoContable> findByEstado(EstadoAsiento estado, Pageable pageable);
    
    /**
     * Busca asientos por estado y rango de fechas.
     * @param estado Estado del asiento
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de asientos que cumplen los criterios
     */
    @Query("SELECT a FROM AsientoContable a WHERE a.estado = :estado " +
           "AND a.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY a.fecha DESC")
    List<AsientoContable> findByEstadoAndFechaBetween(@Param("estado") EstadoAsiento estado,
                                                       @Param("fechaInicio") LocalDate fechaInicio,
                                                       @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Busca asientos relacionados con una factura.
     * @param facturaId ID de la factura
     * @return Lista de asientos de la factura
     */
    @Query("SELECT a FROM AsientoContable a WHERE a.factura.idFactura = :facturaId")
    List<AsientoContable> findByFacturaId(@Param("facturaId") Long facturaId);
    
    /**
     * Busca asientos relacionados con un pago.
     * @param pagoId ID del pago
     * @return Lista de asientos del pago
     */
    @Query("SELECT a FROM AsientoContable a WHERE a.pago.idPago = :pagoId")
    List<AsientoContable> findByPagoId(@Param("pagoId") Long pagoId);
    
    /**
     * Busca asientos por concepto (búsqueda parcial).
     * @param concepto Concepto o parte del concepto
     * @return Lista de asientos que coinciden
     */
    @Query("SELECT a FROM AsientoContable a WHERE LOWER(a.concepto) LIKE LOWER(CONCAT('%', :concepto, '%')) ORDER BY a.fecha DESC")
    List<AsientoContable> findByConceptoContaining(@Param("concepto") String concepto);
    
    /**
     * Busca todos los asientos ordenados por fecha descendente.
     * @param pageable Configuración de paginación
     * @return Página de asientos
     */
    Page<AsientoContable> findAllByOrderByFechaDesc(Pageable pageable);
    
    /**
     * Obtiene el último número de asiento del año.
     * @param anio Año del asiento
     * @return Último número o null si no hay
     */
    @Query("SELECT a.numero FROM AsientoContable a WHERE a.numero LIKE CONCAT('ASI-', :anio, '-%') " +
           "ORDER BY a.numero DESC LIMIT 1")
    Optional<String> findUltimoNumeroDelAnio(@Param("anio") String anio);
    
    /**
     * Cuenta asientos por estado.
     * @param estado Estado del asiento
     * @return Cantidad de asientos
     */
    Long countByEstado(EstadoAsiento estado);
    
    /**
     * Cuenta asientos en un rango de fechas.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Cantidad de asientos
     */
    @Query("SELECT COUNT(a) FROM AsientoContable a WHERE a.fecha BETWEEN :fechaInicio AND :fechaFin")
    Long countByFechaBetween(@Param("fechaInicio") LocalDate fechaInicio, 
                             @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Busca asientos del mes actual.
     * @param anio Año
     * @param mes Mes (1-12)
     * @return Lista de asientos del mes
     */
    @Query("SELECT a FROM AsientoContable a WHERE YEAR(a.fecha) = :anio AND MONTH(a.fecha) = :mes " +
           "ORDER BY a.fecha, a.numero")
    List<AsientoContable> findAsientosDelMes(@Param("anio") int anio, @Param("mes") int mes);
    
    /**
     * Busca asientos contabilizados en un período.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de asientos contabilizados
     */
    @Query("SELECT a FROM AsientoContable a WHERE a.estado = 'CONTABILIZADO' " +
           "AND a.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY a.fecha, a.numero")
    List<AsientoContable> findAsientosContabilizados(@Param("fechaInicio") LocalDate fechaInicio,
                                                      @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Verifica si existe un asiento automático para una factura.
     * @param facturaId ID de la factura
     * @return true si existe
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AsientoContable a WHERE a.factura.idFactura = :facturaId AND a.tipo = 'AUTOMATICO_VENTA'")
    boolean existeAsientoVentaParaFactura(@Param("facturaId") Long facturaId);
    
    /**
     * Verifica si existe un asiento automático para un pago.
     * @param pagoId ID del pago
     * @return true si existe
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AsientoContable a WHERE a.pago.idPago = :pagoId AND a.tipo = 'AUTOMATICO_PAGO'")
    boolean existeAsientoPagoParaPago(@Param("pagoId") Long pagoId);
    
    /**
     * Busca asientos borradores antiguos (para limpieza).
     * @param fechaLimite Fecha límite
     * @return Lista de borradores antiguos
     */
    @Query("SELECT a FROM AsientoContable a WHERE a.estado = 'BORRADOR' " +
           "AND a.fechaCreacion < :fechaLimite")
    List<AsientoContable> findBorradoresAntiguos(@Param("fechaLimite") LocalDate fechaLimite);
}
