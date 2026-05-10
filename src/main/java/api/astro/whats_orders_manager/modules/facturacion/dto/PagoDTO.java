package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferir datos de pagos entre capas de la aplicación.
 * Incluye validaciones de negocio.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoDTO {
    
    private Long idPago;
    
    private String numeroPago;
    
    @NotNull(message = "El ID del cliente es obligatorio")
    private Integer idCliente;
    
    private Integer idFactura;
    
    @NotNull(message = "El tipo de pago es obligatorio")
    @Builder.Default
    private TipoPago tipoPago = TipoPago.TOTAL;
    
    // Descripción del tipo de pago para mostrar
    private String tipoPagoDescripcion;
    
    // Datos de la factura para mostrar en vistas
    private String numeroFactura;
    private String nombreCliente;
    private BigDecimal totalFactura;
    private BigDecimal saldoPendiente;
    
    @NotNull(message = "El monto del pago es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    
    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;
    
    // Descripción del método de pago para mostrar
    private String metodoPagoDescripcion;
    
    @NotNull(message = "La fecha de pago es obligatoria")
    private LocalDateTime fechaPago;
    
    private String referenciaBancaria;
    
    private String banco;
    
    private String cuentaBancaria;
    
    private String comprobanteUrl;
    
    @NotNull(message = "El estado del pago es obligatorio")
    private EstadoPago estado;
    
    // Descripción del estado para mostrar
    private String estadoDescripcion;
    
    private String observaciones;
    
    @Builder.Default
    private Boolean conciliado = false;
    
    private LocalDateTime fechaConciliacion;
    
    // Campos de auditoría
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;
    
    // Campos de anulación
    private Integer anuladoPor;
    private LocalDateTime anuladoEn;
    private String motivoAnulacion;
    
    /**
     * Constructor para proyección JPQL optimizada.
     * Usado en consultas que retornan DTOs directamente.
     */
    public PagoDTO(Long idPago, String numeroPago, BigDecimal monto, MetodoPago metodoPago,
                   EstadoPago estado, TipoPago tipoPago, LocalDateTime fechaPago,
                   String referenciaBancaria, String observaciones, Boolean conciliado,
                   LocalDateTime fechaConciliacion, Integer idCliente, String nombreCliente,
                   Integer idFactura, String numeroFactura, BigDecimal totalFactura,
                   BigDecimal saldoPendiente) {
        this.idPago = idPago;
        this.numeroPago = numeroPago;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.metodoPagoDescripcion = metodoPago != null ? metodoPago.getDescripcion() : null;
        this.estado = estado;
        this.estadoDescripcion = estado != null ? estado.getDescripcion() : null;
        this.tipoPago = tipoPago;
        this.tipoPagoDescripcion = tipoPago != null ? tipoPago.getDescripcion() : null;
        this.fechaPago = fechaPago;
        this.referenciaBancaria = referenciaBancaria;
        this.observaciones = observaciones;
        this.conciliado = conciliado;
        this.fechaConciliacion = fechaConciliacion;
        this.idCliente = idCliente;
        this.nombreCliente = nombreCliente;
        this.idFactura = idFactura;
        this.numeroFactura = numeroFactura;
        this.totalFactura = totalFactura;
        this.saldoPendiente = saldoPendiente;
    }
    
    /**
     * Verifica si el pago requiere referencia según el método de pago.
     * @return true si requiere referencia
     */
    public boolean requiereReferencia() {
        return metodoPago != null && metodoPago.requiereReferencia();
    }
    
    /**
     * Verifica si el pago es válido para cálculos.
     * @return true si está confirmado o conciliado
     */
    public boolean esValido() {
        return estado != null && estado.esValido();
    }
    
    /**
     * Obtiene el código del método de pago para Hacienda.
     * @return Código (01-99)
     */
    public String getCodigoMetodoPago() {
        return metodoPago != null ? metodoPago.getCodigo() : null;
    }
    
    /**
     * Verifica si el pago está conciliado.
     * @return true si está conciliado
     */
    public boolean estaConciliado() {
        return Boolean.TRUE.equals(conciliado) || estado == EstadoPago.CONCILIADO;
    }
    
    /**
     * Verifica si el pago está rechazado.
     * @return true si está rechazado
     */
    public boolean estaRechazado() {
        return estado == EstadoPago.RECHAZADO;
    }
    
    /**
     * Verifica si el pago está anulado.
     * @return true si está anulado
     */
    public boolean estaAnulado() {
        return estado == EstadoPago.ANULADO;
    }
    
    /**
     * Verifica si el pago puede ser editado.
     * Los pagos conciliados o anulados no pueden editarse.
     * @return true si puede editarse
     */
    public boolean puedeEditarse() {
        return estado != EstadoPago.CONCILIADO && estado != EstadoPago.ANULADO;
    }
    
    /**
     * Verifica si el pago puede ser eliminado.
     * Los pagos conciliados o anulados no pueden eliminarse.
     * @return true si puede eliminarse
     */
    public boolean puedeEliminarse() {
        return estado != EstadoPago.CONCILIADO && estado != EstadoPago.ANULADO;
    }
}
