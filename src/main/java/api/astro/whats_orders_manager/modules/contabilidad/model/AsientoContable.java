package api.astro.whats_orders_manager.modules.contabilidad.model;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un asiento contable en el sistema de doble partida.
 * Agrupa un conjunto de movimientos (débitos y créditos) que deben cuadrar.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Entity
@Table(name = "asientos_contables", indexes = {
    @Index(name = "idx_asiento_numero", columnList = "numero", unique = true),
    @Index(name = "idx_asiento_fecha", columnList = "fecha"),
    @Index(name = "idx_asiento_tipo", columnList = "tipo"),
    @Index(name = "idx_asiento_estado", columnList = "estado"),
    @Index(name = "idx_asiento_factura", columnList = "factura_id"),
    @Index(name = "idx_asiento_pago", columnList = "pago_id"),
    @Index(name = "idx_asiento_cpp", columnList = "cuenta_por_pagar_id"),
    @Index(name = "idx_asiento_pago_proveedor", columnList = "pago_proveedor_id")
})
@Getter
@Setter
@ToString(exclude = {"detalles", "factura", "pago", "cuentaPorPagar", "pagoProveedor", "nomina"})
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class AsientoContable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idAsiento;
    
    /**
     * Número consecutivo del asiento (Ej: ASI-2026-0001).
     */
    @Size(max = 50, message = "El número no puede exceder 50 caracteres")
    @Column(unique = true, length = 50)
    private String numero;
    
    /**
     * Fecha del asiento contable.
     */
    @NotNull(message = "La fecha del asiento es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;
    
    /**
     * Descripción o concepto del asiento.
     */
    @NotNull(message = "El concepto del asiento es obligatorio")
    @Size(max = 500, message = "El concepto no puede exceder 500 caracteres")
    @Column(nullable = false, length = 500)
    private String concepto;
    
    /**
     * Tipo de asiento (manual, automático, etc.).
     */
    @NotNull(message = "El tipo de asiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAsiento tipo = TipoAsiento.MANUAL;
    
    /**
     * Estado del asiento (borrador, contabilizado, anulado).
     */
    @NotNull(message = "El estado del asiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsiento estado = EstadoAsiento.BORRADOR;
    
    /**
     * Detalles del asiento (líneas de débito y crédito).
     */
    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAsiento> detalles = new ArrayList<>();
    
    // ==================== REFERENCIAS OPCIONALES ====================
    
    /**
     * Factura relacionada (si el asiento fue generado por una venta).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;
    
    /**
     * Pago relacionado (si el asiento fue generado por un pago).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    /**
     * Cuenta por pagar origen (set when generated from a CPP creation).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id")
    private CuentaPorPagar cuentaPorPagar;

    /**
     * Pago a proveedor origen (set when generated from a supplier payment).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_proveedor_id")
    private PagoProveedor pagoProveedor;

    /**
     * Nómina origen (set when generated from a payroll run).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomina_id")
    private Nomina nomina;

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

    private String motivoAnulacion;
    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Calcula el total del debe (suma de todos los débitos).
     * @return Total del debe
     */
    @Transient
    public BigDecimal getTotalDebe() {
        return detalles.stream()
            .map(DetalleAsiento::getDebe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula el total del haber (suma de todos los créditos).
     * @return Total del haber
     */
    @Transient
    public BigDecimal getTotalHaber() {
        return detalles.stream()
            .map(DetalleAsiento::getHaber)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Verifica si el asiento está cuadrado (debe = haber).
     * @return true si el total del debe es igual al total del haber
     */
    @Transient
    public boolean estaCuadrado() {
        return getTotalDebe().compareTo(getTotalHaber()) == 0;
    }
    
    /**
     * Verifica si el asiento tiene al menos 2 líneas.
     * @return true si tiene 2 o más detalles
     */
    @Transient
    public boolean tieneDetallesSuficientes() {
        return detalles != null && detalles.size() >= 2;
    }
    
    /**
     * Verifica si el asiento puede ser modificado.
     * @return true si está en estado BORRADOR
     */
    public boolean puedeModificarse() {
        return estado != null && estado.puedeModificarse();
    }
    
    /**
     * Verifica si el asiento puede ser contabilizado.
     * @return true si está cuadrado, tiene detalles suficientes y está en BORRADOR
     */
    public boolean puedeContabilizarse() {
        return estado == EstadoAsiento.BORRADOR 
            && estaCuadrado() 
            && tieneDetallesSuficientes();
    }
    
    /**
     * Verifica si el asiento puede ser anulado.
     * @return true si está CONTABILIZADO
     */
    public boolean puedeAnularse() {
        return estado == EstadoAsiento.CONTABILIZADO;
    }
    
    /**
     * Agrega un detalle al asiento.
     * @param detalle Detalle a agregar
     */
    public void agregarDetalle(DetalleAsiento detalle) {
        detalles.add(detalle);
        detalle.setAsiento(this);
    }
    
    /**
     * Elimina un detalle del asiento.
     * @param detalle Detalle a eliminar
     */
    public void eliminarDetalle(DetalleAsiento detalle) {
        detalles.remove(detalle);
        detalle.setAsiento(null);
    }
    
    /**
     * Limpia todos los detalles del asiento.
     */
    public void limpiarDetalles() {
        detalles.clear();
    }
    
    /**
     * Contabiliza el asiento (cambia estado a CONTABILIZADO).
     * @throws IllegalStateException si no puede contabilizarse
     */
    public void contabilizar() {
        if (!puedeContabilizarse()) {
            throw new IllegalStateException("El asiento no puede ser contabilizado. Verifique que esté cuadrado y tenga al menos 2 líneas.");
        }
        this.estado = EstadoAsiento.CONTABILIZADO;
    }
    
    /**
     * Anula el asiento (cambia estado a ANULADO).
     * @throws IllegalStateException si no puede anularse
     */
    public void anular(String motivo, Integer usuario) {
        if (!puedeAnularse()) {
            throw new IllegalStateException("El asiento no puede ser anulado. Solo los asientos contabilizados pueden anularse.");
        }
        this.estado = EstadoAsiento.ANULADO;
        this.modificadoPor = usuario;
        this.motivoAnulacion = motivo;
    }
}
