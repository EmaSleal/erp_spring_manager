package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un pago aplicado a una factura.
 * Soporta pagos parciales y múltiples métodos de pago por factura.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Entity
@Table(name = "pagos", indexes = {
    @Index(name = "idx_pago_numero", columnList = "numeroPago", unique = true),
    @Index(name = "idx_pago_cliente", columnList = "clienteId"),
    @Index(name = "idx_pago_factura", columnList = "idFactura"),
    @Index(name = "idx_pago_fecha", columnList = "fechaPago"),
    @Index(name = "idx_pago_metodo", columnList = "metodoPago"),
    @Index(name = "idx_pago_estado", columnList = "estado"),
    @Index(name = "idx_pago_tipo", columnList = "tipoPago")
})
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Pago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago")
    private Long idPago;
    
    /**
     * Número consecutivo único del pago (PAG-YYYYMMDD-0001).
     * Se genera automáticamente al crear el pago.
     */
    @Column(name = "numeroPago", unique = true, length = 20, nullable = false)
    private String numeroPago;
    
    /**
     * Cliente que realiza el pago.
     * Relación directa necesaria para adelantos sin factura asignada.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clienteId", nullable = false)
    @ToString.Exclude
    private Cliente cliente;
    
    /**
     * Factura a la que se aplica el pago.
     * Puede ser NULL para adelantos/anticipos sin factura asignada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idFactura")
    @ToString.Exclude
    @JsonIgnore
    private Factura factura;
    
    /**
     * Tipo de pago según su aplicación.
     */
    @NotNull(message = "El tipo de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipoPago", nullable = false, length = 20)
    private TipoPago tipoPago = TipoPago.TOTAL;
    
    /**
     * Monto del pago. Debe ser mayor a 0 y no exceder el saldo pendiente.
     */
    @NotNull(message = "El monto del pago es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;
    
    /**
     * Método de pago utilizado (según catálogo Hacienda CR).
     */
    @NotNull(message = "El método de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;
    
    /**
     * Fecha y hora en que se realizó el pago.
     */
    @NotNull(message = "La fecha de pago es obligatoria")
    @Column(nullable = false)
    private LocalDateTime fechaPago;
    
    /**
     * Referencia bancaria del pago (número de cheque, transacción, comprobante, etc.).
     * Obligatorio para ciertos métodos de pago (CHEQUE, TRANSFERENCIA).
     */
    @Column(name = "referenciaBancaria", length = 100)
    private String referenciaBancaria;
    
    /**
     * Nombre del banco donde se realizó el pago.
     */
    @Column(name = "banco", length = 100)
    private String banco;
    
    /**
     * Últimos dígitos de la cuenta bancaria utilizada.
     */
    @Column(name = "cuentaBancaria", length = 50)
    private String cuentaBancaria;
    
    /**
     * Estado actual del pago.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.CONFIRMADO;
    
    /**
     * Notas u observaciones adicionales sobre el pago.
     */
    @Column(name = "observaciones", length = 1000)
    private String observaciones;
    
    /**
     * URL del comprobante de pago digitalizado.
     */
    @Column(name = "comprobanteUrl", length = 255)
    private String comprobanteUrl;
    
    /**
     * Indica si el pago fue conciliado con el banco.
     */
    @Column(nullable = false)
    private Boolean conciliado = false;
    
    /**
     * Fecha de conciliación bancaria (si aplica).
     */
    @Column
    private LocalDateTime fechaConciliacion;
    
    // ==================== AUDITORÍA ====================
    
    @CreatedBy
    @Column(updatable = false)
    private Integer creadoPor;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;
    
    @LastModifiedBy
    @Column
    private Integer modificadoPor;
    
    @LastModifiedDate
    @Column
    private LocalDateTime fechaModificacion;
    
    // ==================== CAMPOS DE ANULACIÓN ====================
    
    /**
     * ID del usuario que anuló el pago.
     */
    @Column
    private Integer anuladoPor;
    
    /**
     * Fecha y hora en que se anuló el pago.
     */
    @Column
    private LocalDateTime anuladoEn;
    
    /**
     * Motivo de la anulación del pago.
     */
    @Column(length = 1000)
    private String motivoAnulacion;
    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Verifica si el pago es válido para cálculos de saldo.
     * @return true si el estado es CONFIRMADO o CONCILIADO
     */
    public boolean esValido() {
        return estado != null && estado.esValido();
    }
    
    /**
     * Marca el pago como conciliado.
     */
    public void conciliar() {
        this.conciliado = true;
        this.fechaConciliacion = LocalDateTime.now();
        this.estado = EstadoPago.CONCILIADO;
    }
    
    /**
     * Rechaza el pago.
     * @param motivo Motivo del rechazo
     */
    public void rechazar(String motivo) {
        this.estado = EstadoPago.RECHAZADO;
        this.observaciones = (this.observaciones != null ? this.observaciones + " | " : "") + "RECHAZADO: " + motivo;
    }
    
    /**
     * Anula el pago.
     * @param motivo Motivo de la anulación
     * @param usuarioId ID del usuario que anula
     */
    public void anular(String motivo, Integer usuarioId) {
        this.estado = EstadoPago.ANULADO;
        this.anuladoPor = usuarioId;
        this.anuladoEn = LocalDateTime.now();
        this.motivoAnulacion = motivo;
    }
    
    /**
     * Valida que el pago no exceda el saldo pendiente de la factura.
     * @throws IllegalStateException si el monto excede el saldo
     */
    public void validarMonto() {
        // Los adelantos no requieren factura
        if (tipoPago != null && tipoPago.esAdelanto()) {
            return;
        }
        
        if (factura != null) {
            BigDecimal saldoPendiente = factura.calcularSaldoPendiente();
            if (monto.compareTo(saldoPendiente) > 0) {
                throw new IllegalStateException(
                    String.format("El monto del pago (%.2f) excede el saldo pendiente (%.2f)",
                        monto, saldoPendiente)
                );
            }
        }
    }
    
    /**
     * Valida que si el tipo de pago requiere factura, esta esté presente.
     * @throws IllegalStateException si falta la factura obligatoria
     */
    public void validarFacturaRequerida() {
        if (tipoPago != null && tipoPago.requiereFactura() && factura == null) {
            throw new IllegalStateException(
                "El tipo de pago " + tipoPago.getDescripcion() + " requiere una factura asignada"
            );
        }
    }
    
    /**
     * Valida que si el método de pago requiere referencia, esta esté presente.
     * @throws IllegalStateException si falta la referencia obligatoria
     */
    public void validarReferencia() {
        if (metodoPago != null && metodoPago.requiereReferencia()) {
            if (referenciaBancaria == null || referenciaBancaria.trim().isEmpty()) {
                throw new IllegalStateException(
                    "El método de pago " + metodoPago.getDescripcion() + " requiere una referencia"
                );
            }
        }
    }
    
    @PrePersist
    @PreUpdate
    protected void validarAntesPersistir() {
        if (fechaPago == null) {
            fechaPago = LocalDateTime.now();
        }
        
        // Validar que la fecha de pago no sea futura
        if (fechaPago.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La fecha de pago no puede ser futura");
        }
        
        validarReferencia();
        validarFacturaRequerida();
        // Note: validarMonto() se debe llamar desde el servicio ya que requiere acceso completo a la factura
    }
}
