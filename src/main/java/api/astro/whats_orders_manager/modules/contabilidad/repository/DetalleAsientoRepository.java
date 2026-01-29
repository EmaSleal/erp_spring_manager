package api.astro.whats_orders_manager.modules.contabilidad.repository;

import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la entidad DetalleAsiento.
 * Gestiona las líneas individuales de los asientos contables.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Repository
public interface DetalleAsientoRepository extends JpaRepository<DetalleAsiento, Long> {
    
    /**
     * Busca los detalles de un asiento específico.
     * @param asientoId ID del asiento
     * @return Lista de detalles del asiento
     */
    @Query("SELECT d FROM DetalleAsiento d WHERE d.asiento.idAsiento = :asientoId ORDER BY d.idDetalle")
    List<DetalleAsiento> findByAsientoId(@Param("asientoId") Long asientoId);
    
    /**
     * Busca todos los movimientos de una cuenta.
     * @param cuentaId ID de la cuenta
     * @return Lista de movimientos de la cuenta
     */
    @Query("SELECT d FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId " +
           "ORDER BY d.asiento.fecha DESC, d.asiento.numero DESC")
    List<DetalleAsiento> findByCuentaId(@Param("cuentaId") Long cuentaId);
    
    /**
     * Busca movimientos de una cuenta en un período.
     * @param cuentaId ID de la cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de movimientos en el período
     */
    @Query("SELECT d FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO' " +
           "ORDER BY d.asiento.fecha, d.asiento.numero")
    List<DetalleAsiento> findMovimientosPorCuentaYPeriodo(@Param("cuentaId") Long cuentaId,
                                                           @Param("fechaInicio") LocalDate fechaInicio,
                                                           @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Calcula el saldo de una cuenta hasta una fecha.
     * @param cuentaId ID de la cuenta
     * @param fecha Fecha límite
     * @return Saldo (debe - haber)
     */
    @Query("SELECT COALESCE(SUM(d.debe), 0) - COALESCE(SUM(d.haber), 0) " +
           "FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.fecha <= :fecha " +
           "AND d.asiento.estado = 'CONTABILIZADO'")
    BigDecimal calcularSaldoCuentaHastaFecha(@Param("cuentaId") Long cuentaId, 
                                              @Param("fecha") LocalDate fecha);
    
    /**
     * Calcula el saldo de una cuenta en un período.
     * @param cuentaId ID de la cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Saldo del período
     */
    @Query("SELECT COALESCE(SUM(d.debe), 0) - COALESCE(SUM(d.haber), 0) " +
           "FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO'")
    BigDecimal calcularSaldoCuentaPeriodo(@Param("cuentaId") Long cuentaId,
                                          @Param("fechaInicio") LocalDate fechaInicio,
                                          @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Calcula el total de débitos de una cuenta en un período.
     * @param cuentaId ID de la cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Total débitos
     */
    @Query("SELECT COALESCE(SUM(d.debe), 0) FROM DetalleAsiento d " +
           "WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO'")
    BigDecimal calcularTotalDebitosCuenta(@Param("cuentaId") Long cuentaId,
                                          @Param("fechaInicio") LocalDate fechaInicio,
                                          @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Calcula el total de créditos de una cuenta en un período.
     * @param cuentaId ID de la cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Total créditos
     */
    @Query("SELECT COALESCE(SUM(d.haber), 0) FROM DetalleAsiento d " +
           "WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO'")
    BigDecimal calcularTotalCreditosCuenta(@Param("cuentaId") Long cuentaId,
                                           @Param("fechaInicio") LocalDate fechaInicio,
                                           @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Obtiene el libro mayor de una cuenta (movimientos contabilizados).
     * @param cuentaId ID de la cuenta
     * @return Lista de movimientos contabilizados
     */
    @Query("SELECT d FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.estado = 'CONTABILIZADO' " +
           "ORDER BY d.asiento.fecha, d.asiento.numero")
    List<DetalleAsiento> obtenerLibroMayorCuenta(@Param("cuentaId") Long cuentaId);
    
    /**
     * Obtiene el libro mayor de una cuenta en un período.
     * @param cuentaId ID de la cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de movimientos del período
     */
    @Query("SELECT d FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO' " +
           "ORDER BY d.asiento.fecha, d.asiento.numero")
    List<DetalleAsiento> obtenerLibroMayorCuentaPeriodo(@Param("cuentaId") Long cuentaId,
                                                         @Param("fechaInicio") LocalDate fechaInicio,
                                                         @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Cuenta los movimientos de una cuenta.
     * @param cuentaId ID de la cuenta
     * @return Cantidad de movimientos
     */
    @Query("SELECT COUNT(d) FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId " +
           "AND d.asiento.estado = 'CONTABILIZADO'")
    Long contarMovimientosCuenta(@Param("cuentaId") Long cuentaId);
    
    /**
     * Busca cuentas con movimientos en un período.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de IDs de cuentas con movimientos
     */
    @Query("SELECT DISTINCT d.cuenta.idCuenta FROM DetalleAsiento d " +
           "WHERE d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO'")
    List<Long> findCuentasConMovimientos(@Param("fechaInicio") LocalDate fechaInicio,
                                          @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Obtiene el detalle del balance de comprobación.
     * Para cada cuenta con movimientos, suma débitos y créditos.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de arrays [cuentaId, totalDebe, totalHaber]
     */
    @Query("SELECT d.cuenta.idCuenta, SUM(d.debe), SUM(d.haber) " +
           "FROM DetalleAsiento d WHERE d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO' " +
           "GROUP BY d.cuenta.idCuenta " +
           "ORDER BY d.cuenta.codigo")
    List<Object[]> obtenerBalanceComprobacion(@Param("fechaInicio") LocalDate fechaInicio,
                                               @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Calcula totales globales de un período.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Array [totalDebe, totalHaber]
     */
    @Query("SELECT SUM(d.debe), SUM(d.haber) FROM DetalleAsiento d " +
           "WHERE d.asiento.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "AND d.asiento.estado = 'CONTABILIZADO'")
    Object[] calcularTotalesPeriodo(@Param("fechaInicio") LocalDate fechaInicio,
                                    @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Busca detalles por descripción (búsqueda parcial).
     * @param descripcion Descripción o parte de ella
     * @return Lista de detalles que coinciden
     */
    @Query("SELECT d FROM DetalleAsiento d WHERE LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%')) " +
           "ORDER BY d.asiento.fecha DESC")
    List<DetalleAsiento> findByDescripcionContaining(@Param("descripcion") String descripcion);
    
    /**
     * Elimina todos los detalles de un asiento (usado en cascada manual).
     * @param asientoId ID del asiento
     */
    @Query("DELETE FROM DetalleAsiento d WHERE d.asiento.idAsiento = :asientoId")
    void deleteByAsientoId(@Param("asientoId") Long asientoId);



}
