package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * Suma el total de facturas no entregadas (pendientes), neto de lo acreditado por notas de crédito parciales
     * (T-047); no cuenta anuladas ni acreditadas totalmente.
     */
    @Query("SELECT COALESCE(SUM(f.total - f.montoAcreditado), 0) FROM Factura f WHERE f.entregado = false " +
           "AND f.anulada = false AND f.acreditadaTotalmente = false")
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
     * Criterios: fechaPago < hoy (la fecha de hoy la calcula la aplicación con su Clock, T-050), entregado = true, no anulada ni acreditada totalmente (T-047), cliente con email
     * @return Lista de facturas con pago vencido
     */
    @Query("SELECT f FROM Factura f " +
           "WHERE f.fechaPago < :hoy " +
           "AND f.entregado = true " +
           "AND f.anulada = false AND f.acreditadaTotalmente = false " +
           "AND f.cliente.email IS NOT NULL")
    List<Factura> findFacturasConPagoVencido(@Param("hoy") java.sql.Date hoy);
    
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

    /**
     * Factura con bloqueo de escritura (SELECT ... FOR UPDATE) hasta el fin de la transacción: serializa la emisión de
     * notas sobre una misma factura (T-032).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Factura f WHERE f.idFactura = :idFactura")
    Optional<Factura> findByIdParaActualizar(@Param("idFactura") Integer idFactura);

    //findByClienteId(idCliente)
    @Query("SELECT f FROM Factura f WHERE f.cliente.idCliente = :idCliente")
    Optional<List<Factura>> findByClienteId(@Param("idCliente") Integer idCliente);

    /**
     * Predicado compartido de «factura anulada que admite sustituta y no la tiene» (T-043, ADR-048 y ADR-045), usado por
     * el contador del panel y por el filtro {@code sustitutaPendiente} del listado, para que coincidan. Espejo en JPQL
     * de {@code SustitucionElegibilidad.evaluar} más la regla de ambiente de T-044 (puedeEmitirSustituta): anulada; la
     * primera nota CREDITO APLICADA con código 01 o 13 (la de menor id); el comprobante vigente de esa nota ACEPTADO; el
     * comprobante de origen que la nota referencia ACEPTADO con clave y fecha de emisión y, si se indica
     * {@code :ambiente}, de ese ambiente; y ninguna sustituta no anulada. Con :ambiente null no filtra por ambiente.
     * Requiere el alias {@code f} sobre Factura y el parámetro {@code :ambiente}.
     */
    String PREDICADO_SUSTITUTA_PENDIENTE = "f.anulada = true "
        + "AND NOT EXISTS (SELECT 1 FROM Factura s WHERE s.facturaSustituida = f AND s.anulada = false) "
        + "AND EXISTS (SELECT 1 FROM NotaFactura n JOIN n.comprobanteReferencia cr "
        + "WHERE n.id = (SELECT MIN(n2.id) FROM NotaFactura n2 WHERE n2.factura = f "
        + "AND n2.tipo = api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura.CREDITO "
        + "AND n2.estado = api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura.APLICADA AND n2.codigoReferencia IN ('01', '13')) "
        + "AND EXISTS (SELECT 1 FROM ComprobanteElectronico cn WHERE cn.notaFactura = n AND cn.vigente = true "
        + "AND cn.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ACEPTADO) "
        + "AND cr.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ACEPTADO "
        + "AND cr.claveNumerica IS NOT NULL AND TRIM(cr.claveNumerica) <> '' AND cr.fechaEmision IS NOT NULL "
        + "AND (:ambiente IS NULL OR cr.ambiente = :ambiente))";

    /**
     * Facturas no anuladas y sin ningún comprobante (de cualquier ambiente), las que se pueden emitir (T-043). T-057: las del
     * régimen simplificado no se emiten a Hacienda y no cuentan como pendientes de emitir.
     */
    String PREDICADO_SIN_COMPROBANTE = "f.anulada = false "
        + "AND f.regimenEmisor <> api.astro.whats_orders_manager.modules.regimen.enums.Regimen.SIMPLIFICADO AND NOT EXISTS "
        + "(SELECT 1 FROM ComprobanteElectronico x WHERE x.factura = f)";

    /**
     * Busca facturas paginadas aplicando filtros opcionales del listado.
     * Cada filtro se ignora cuando su parámetro es null.
     * sinFE y estadoFE son mutuamente excluyentes: sinFE busca facturas no anuladas sin ningún comprobante electrónico
     * (T-043: las que se pueden emitir; las de otro ambiente con comprobante no cuentan), mientras que estadoFE filtra
     * por el estado del comprobante vigente. sustitutaPendiente (T-043) deja las anuladas que admiten sustituta y no la
     * tienen en el {@code ambiente} indicado (null = todos), con el mismo predicado que el contador del panel.
     */
    @Query(value = "SELECT f FROM Factura f LEFT JOIN f.comprobantes c ON c.vigente = true " +
           "WHERE (:startDate IS NULL OR CAST(f.fechaEntrega AS date) >= :startDate) " +
           "AND (:endDate IS NULL OR CAST(f.fechaEntrega AS date) <= :endDate) " +
           "AND (:entregado IS NULL OR f.entregado = :entregado) " +
           "AND (:pagada IS NULL OR (:pagada = true AND f.estadoPago = 'PAGADO_TOTAL') OR (:pagada = false AND f.estadoPago <> 'PAGADO_TOTAL' AND f.acreditadaTotalmente = false)) " +
           "AND (:sinFE IS NULL OR (" + PREDICADO_SIN_COMPROBANTE + ")) " +
           "AND (:estadoFE IS NULL OR c.estado = :estadoFE) " +
           "AND (:sustitutaPendiente IS NULL OR (" + PREDICADO_SUSTITUTA_PENDIENTE + "))",
           countQuery = "SELECT COUNT(f) FROM Factura f LEFT JOIN f.comprobantes c ON c.vigente = true " +
           "WHERE (:startDate IS NULL OR CAST(f.fechaEntrega AS date) >= :startDate) " +
           "AND (:endDate IS NULL OR CAST(f.fechaEntrega AS date) <= :endDate) " +
           "AND (:entregado IS NULL OR f.entregado = :entregado) " +
           "AND (:pagada IS NULL OR (:pagada = true AND f.estadoPago = 'PAGADO_TOTAL') OR (:pagada = false AND f.estadoPago <> 'PAGADO_TOTAL' AND f.acreditadaTotalmente = false)) " +
           "AND (:sinFE IS NULL OR (" + PREDICADO_SIN_COMPROBANTE + ")) " +
           "AND (:estadoFE IS NULL OR c.estado = :estadoFE) " +
           "AND (:sustitutaPendiente IS NULL OR (" + PREDICADO_SUSTITUTA_PENDIENTE + "))")
    Page<Factura> buscarConFiltros(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("entregado") Boolean entregado,
            @Param("pagada") Boolean pagada,
            @Param("sinFE") Boolean sinFE,
            @Param("estadoFE") EstadoComprobante estadoFE,
            @Param("sustitutaPendiente") Boolean sustitutaPendiente,
            @Param("ambiente") AmbienteHacienda ambiente,
            Pageable pageable
    );

    /** Sustituta no anulada de una factura (ADR-045 §1); a lo sumo una (V43). */
    Optional<Factura> findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(Integer idFactura);

    /**
     * T-043 (ADR-048): facturas no anuladas sin ningún comprobante, las mismas que lista
     * {@code /facturas?estadoFE=sin_fe} ({@link #PREDICADO_SIN_COMPROBANTE}). No lleva ambiente: la que ya tiene un
     * comprobante de otro ambiente no se puede emitir aquí (T-044) y no cuenta.
     */
    @Query("SELECT COUNT(f) FROM Factura f WHERE " + PREDICADO_SIN_COMPROBANTE)
    long contarSinComprobante();

    /**
     * T-043 (ADR-048): facturas anuladas que admiten sustituta y no la tienen, en el ambiente indicado (null = todos).
     * Mismo predicado que el filtro {@code sustitutaPendiente} del listado ({@link #PREDICADO_SUSTITUTA_PENDIENTE}).
     */
    @Query("SELECT COUNT(f) FROM Factura f WHERE " + PREDICADO_SUSTITUTA_PENDIENTE)
    long contarAnuladasQueAdmitenSustituta(@Param("ambiente") AmbienteHacienda ambiente);
}
