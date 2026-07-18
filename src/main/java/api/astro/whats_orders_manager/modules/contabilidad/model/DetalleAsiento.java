package api.astro.whats_orders_manager.modules.contabilidad.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

/**
 * Entidad que representa una línea de detalle de un asiento contable.
 * Cada línea representa un débito o un crédito en una cuenta específica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Entity
@Table(name = "detalles_asiento", indexes = {
    @Index(name = "idx_detalle_asiento", columnList = "asiento_id"),
    @Index(name = "idx_detalle_cuenta", columnList = "cuenta_id")
})
@Getter
@Setter
@ToString(exclude = {"asiento", "cuenta"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetalleAsiento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idDetalle;
    
    /**
     * Asiento contable al que pertenece este detalle.
     */
    @NotNull(message = "El asiento es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_id", nullable = false)
    private AsientoContable asiento;
    
    /**
     * Cuenta contable afectada por este movimiento.
     */
    @NotNull(message = "La cuenta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaContable cuenta;
    
    /**
     * Importe del débito (columna del DEBE).
     * Solo uno de debe o haber puede ser mayor a cero.
     */
    @NotNull(message = "El debe no puede ser nulo")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal debe = BigDecimal.ZERO;
    
    /**
     * Importe del crédito (columna del HABER).
     * Solo uno de debe o haber puede ser mayor a cero.
     */
    @NotNull(message = "El haber no puede ser nulo")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal haber = BigDecimal.ZERO;
    
    /**
     * Descripción específica de este movimiento.
     */
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Column(length = 500)
    private String descripcion;

    // ==================== AUDITORÍA ====================
    //update_by y create_by se manejan en la clase base AuditableEntity
    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private LocalDateTime createDate;

    @CreatedDate
    @Column(name = "updateDate")
    private LocalDateTime updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @CreatedBy
    @Column(name = "updateBy")
    private Integer modifiedBy;

    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Valida las reglas de negocio del detalle antes de persistir/actualizar.
     * - Debe y haber no pueden ser ambos > 0
     * - Al menos uno debe ser > 0
     * - La cuenta debe poder recibir movimientos
     */
    @PrePersist
    @PreUpdate
    private void validar() {
        // Validar que debe y haber no sean ambos > 0
        if (debe.compareTo(BigDecimal.ZERO) > 0 && haber.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Un detalle no puede tener debe y haber simultáneamente. Solo uno puede ser mayor a cero.");
        }
        
        // Validar que al menos uno sea > 0
        if (debe.compareTo(BigDecimal.ZERO) == 0 && haber.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Debe o haber debe ser mayor a cero.");
        }
        
        // Validar que la cuenta pueda recibir movimientos
        if (cuenta != null && !cuenta.puedeRecibirMovimientos()) {
            throw new IllegalArgumentException(
                String.format("La cuenta %s - %s no puede recibir movimientos. Verifique que esté activa y no sea de agrupación.", 
                    cuenta.getCodigo(), cuenta.getNombre())
            );
        }
    }
    
    /**
     * Verifica si este detalle es un débito.
     * @return true si debe > 0
     */
    public boolean esDebe() {
        return debe.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Verifica si este detalle es un crédito.
     * @return true si haber > 0
     */
    public boolean esHaber() {
        return haber.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Obtiene el monto del movimiento (ya sea debe o haber).
     * @return El monto mayor entre debe y haber
     */
    public BigDecimal getMonto() {
        return debe.compareTo(BigDecimal.ZERO) > 0 ? debe : haber;
    }
    
    /**
     * Establece el detalle como débito.
     * @param monto Monto del débito
     */
    public void setDebe(BigDecimal monto) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            this.debe = monto;
            this.haber = BigDecimal.ZERO;
        }
    }
    
    /**
     * Establece el detalle como crédito.
     * @param monto Monto del crédito
     */
    public void setHaber(BigDecimal monto) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            this.haber = monto;
            this.debe = BigDecimal.ZERO;
        }
    }
    
    /**
     * Invierte el movimiento (convierte débito en crédito y viceversa).
     */
    public void invertir() {
        BigDecimal temp = this.debe;
        this.debe = this.haber;
        this.haber = temp;
    }
}
