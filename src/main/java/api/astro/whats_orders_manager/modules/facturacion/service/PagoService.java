package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.EstadoCuentaClienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.ReporteCajaDTO;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de pagos aplicados a facturas.
 * Maneja el registro, confirmación, conciliación y rechazo de pagos.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
public interface PagoService {
    
    // ==================== OPERACIONES CRUD ====================
    
    /**
     * Obtiene todos los pagos.
     * @return Lista de todos los pagos
     */
    List<Pago> findAll();
    
    /**
     * Obtiene todos los pagos paginados.
     * @param pageable Configuración de paginación
     * @return Página de pagos
     */
    Page<Pago> findAll(Pageable pageable);
    
    /**
     * Obtiene todos los pagos como DTOs paginados.
     * Optimizado para listar sin conversión manual.
     * @param pageable Configuración de paginación
     * @return Página de DTOs
     */
    Page<PagoDTO> findAllAsDTO(Pageable pageable);
    
    /**
     * Busca un pago por su ID.
     * @param id ID del pago
     * @return Optional con el pago si existe
     */
    Optional<Pago> findById(Long id);
    
    /**
     * Elimina un pago por su ID.
     * Actualiza automáticamente el estado de pago de la factura.
     * @param id ID del pago a eliminar
     */
    void deleteById(Long id);
    
    /**
     * Verifica si existe un pago por su ID.
     * @param id ID del pago
     * @return true si existe
     */
    Boolean existsById(Long id);
    
    /**
     * Cuenta todos los pagos.
     * @return Total de pagos
     */
    long count();
    
    // ==================== OPERACIONES DE NEGOCIO ====================
    
    /**
     * Registra un nuevo pago para una factura.
     * Valida que el monto no exceda el saldo pendiente.
     * Actualiza automáticamente el estado de pago de la factura.
     *
     * @param pago Pago a registrar
     * @return Pago guardado
     * @throws IllegalArgumentException si el monto excede el saldo
     * @throws IllegalStateException si falta referencia obligatoria
     */
    Pago registrarPago(Pago pago);

    /**
     * Registers a new payment from a DTO.
     * Resolves Cliente and Factura from the DTO IDs, then delegates to registrarPago(Pago).
     *
     * @param dto PagoDTO with payment data and entity IDs
     * @return saved Pago entity
     * @throws IllegalArgumentException if Cliente or Factura not found
     */
    Pago registrarPago(PagoDTO dto);
    
    /**
     * Confirma un pago pendiente.
     * @param idPago ID del pago a confirmar
     * @return Pago confirmado
     */
    Pago confirmarPago(Long idPago);
    
    /**
     * Concilia un pago con el banco.
     * Marca el pago como conciliado y actualiza la fecha de conciliación.
     * @param idPago ID del pago a conciliar
     * @return Pago conciliado
     */
    Pago conciliarPago(Long idPago);
    
    /**
     * Rechaza un pago.
     * @param idPago ID del pago a rechazar
     * @param motivo Motivo del rechazo
     * @return Pago rechazado
     */
    Pago rechazarPago(Long idPago, String motivo);
    
    /**
     * Anula un pago con trazabilidad completa.
     * Registra quién, cuándo y por qué se anuló el pago.
     * @param idPago ID del pago a anular
     * @param motivo Motivo de la anulación
     * @param usuarioId ID del usuario que anula
     * @return Pago anulado
     */
    Pago anularPago(Long idPago, String motivo, Integer usuarioId);
    
    /**
     * Concilia múltiples pagos en lote.
     * Útil para conciliación bancaria masiva.
     * @param idsPagos Lista de IDs de pagos a conciliar
     * @return Lista de pagos conciliados
     */
    List<Pago> conciliarPagosLote(List<Long> idsPagos);
    
    // ==================== CONSULTAS POR FACTURA ====================
    
    /**
     * Obtiene todos los pagos de una factura.
     * @param idFactura ID de la factura
     * @return Lista de pagos de la factura
     */
    List<Pago> findByFacturaId(Integer idFactura);
    
    /**
     * Obtiene solo los pagos válidos de una factura.
     * @param idFactura ID de la factura
     * @return Lista de pagos válidos (CONFIRMADO o CONCILIADO)
     */
    List<Pago> findPagosValidosByFacturaId(Integer idFactura);
    
    /**
     * Calcula el total pagado de una factura.
     * Solo suma pagos en estado CONFIRMADO o CONCILIADO.
     * @param idFactura ID de la factura
     * @return Total pagado
     */
    BigDecimal calcularTotalPagadoPorFactura(Integer idFactura);
    
    // ==================== CONSULTAS POR CRITERIOS ====================
    
    /**
     * Busca pagos por método de pago.
     * @param metodoPago Método de pago
     * @return Lista de pagos con el método especificado
     */
    List<Pago> findByMetodoPago(MetodoPago metodoPago);
    
    /**
     * Busca pagos por estado.
     * @param estado Estado del pago
     * @return Lista de pagos con el estado especificado
     */
    List<Pago> findByEstado(EstadoPago estado);
    
    /**
     * Busca pagos pendientes de conciliación.
     * @return Lista de pagos confirmados pero no conciliados
     */
    List<Pago> findPagosPendientesConciliacion();
    
    /**
     * Busca pagos en un rango de fechas.
     * @param fechaInicio Fecha inicial
     * @param fechaFin Fecha final
     * @return Lista de pagos en el rango
     */
    List<Pago> findByFechaPagoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
    
    /**
     * Busca un pago por su referencia.
     * @param referencia Referencia del pago
     * @return Optional con el pago si existe
     */
    Optional<Pago> findByReferencia(String referencia);
    
    /**
     * Verifica si existe un pago con la referencia especificada.
     * @param referencia Referencia a verificar
     * @return true si existe
     */
    boolean existsByReferencia(String referencia);
    
    /**
     * Busca un pago por su número único.
     * @param numeroPago Número de pago
     * @return Optional con el pago si existe
     */
    Optional<Pago> findByNumeroPago(String numeroPago);
    
    /**
     * Busca todos los pagos de un cliente.
     * @param idCliente ID del cliente
     * @return Lista de pagos del cliente
     */
    List<Pago> findByClienteId(Integer idCliente);
    
    // ==================== REPORTES Y ESTADÍSTICAS ====================
    
    /**
     * Obtiene el total de ingresos del día actual.
     * @return Total de ingresos del día
     */
    BigDecimal sumIngresosDiaActual();
    
    /**
     * Obtiene todos los pagos del día actual.
     * @return Lista de pagos del día
     */
    List<Pago> findPagosDelDia();
    
    /**
     * Cuenta los pagos del día actual.
     * @return Cantidad de pagos del día
     */
    long countPagosDelDia();
    
    /**
     * Obtiene el total de pagos por método de pago en un rango de fechas.
     * Útil para reportes de caja.
     * @param metodoPago Método de pago
     * @param fechaInicio Fecha inicial
     * @param fechaFin Fecha final
     * @return Total de pagos
     */
    BigDecimal sumByMetodoPagoAndFecha(MetodoPago metodoPago, LocalDateTime fechaInicio, LocalDateTime fechaFin);
    
    /**
     * Cuenta pagos por método de pago en un rango de fechas.
     * @param metodoPago Método de pago
     * @param fechaInicio Fecha inicial
     * @param fechaFin Fecha final
     * @return Cantidad de pagos
     */
    long countByMetodoPagoAndFecha(MetodoPago metodoPago, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    /**
     * Assembles a daily cash report for the given date.
     * Executes a single grouped query for totals (CONFIRMADO + CONCILIADO only)
     * and fetches all payments in the day regardless of estado for detail.
     *
     * @param fecha the report date
     * @return populated ReporteCajaDTO
     */
    ReporteCajaDTO buildReporteCaja(LocalDate fecha);

    /**
     * Assembles the full account statement for a given client.
     * Includes all invoices, all payments, and aggregated totals.
     *
     * @param idCliente client primary key
     * @return populated EstadoCuentaClienteDTO
     * @throws jakarta.persistence.EntityNotFoundException when the client does not exist
     */
    EstadoCuentaClienteDTO buildEstadoCuenta(Integer idCliente);
}
