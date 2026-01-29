package api.astro.whats_orders_manager.modules.contabilidad.model;

import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una cuenta contable en el plan de cuentas.
 * Soporta estructura jerárquica de múltiples niveles.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Entity
@Table(name = "cuentas_contables", indexes = {
    @Index(name = "idx_cuenta_codigo", columnList = "codigo", unique = true),
    @Index(name = "idx_cuenta_tipo", columnList = "tipo"),
    @Index(name = "idx_cuenta_padre", columnList = "cuenta_padre_id"),
    @Index(name = "idx_cuenta_activa", columnList = "activa")
})
@Getter
@Setter
@ToString(exclude = {"cuentaPadre", "subcuentas"})
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CuentaContable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idCuenta;
    
    /**
     * Código único de la cuenta (Ej: 1.1.01.001).
     * Formato jerárquico que indica la posición en el plan de cuentas.
     */
    @NotNull(message = "El código de la cuenta es obligatorio")
    @Pattern(regexp = "^\\d+(\\.\\d+)*$", message = "Formato de código inválido. Use formato: 1.1.01.001")
    @Size(max = 20, message = "El código no puede exceder 20 caracteres")
    @Column(unique = true, nullable = false, length = 20)
    private String codigo;
    
    /**
     * Nombre descriptivo de la cuenta.
     */
    @NotNull(message = "El nombre de la cuenta es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    @Column(nullable = false, length = 200)
    private String nombre;
    
    /**
     * Tipo de cuenta según clasificación contable.
     */
    @NotNull(message = "El tipo de cuenta es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCuenta tipo;
    
    /**
     * Naturaleza de la cuenta (DEUDORA o ACREEDORA).
     */
    @NotNull(message = "La naturaleza de la cuenta es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NaturalezaCuenta naturaleza;
    
    /**
     * Nivel jerárquico en el plan de cuentas (1 = raíz, 2, 3, 4...).
     */
    @NotNull(message = "El nivel es obligatorio")
    @Column(nullable = false)
    private Integer nivel;
    
    /**
     * Cuenta padre en la jerarquía (null para cuentas de nivel 1).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_padre_id")
    private CuentaContable cuentaPadre;
    
    /**
     * Subcuentas dependientes de esta cuenta.
     */
    @OneToMany(mappedBy = "cuentaPadre", cascade = CascadeType.ALL)
    private List<CuentaContable> subcuentas = new ArrayList<>();
    
    /**
     * Indica si la cuenta está activa para su uso.
     */
    @Column(nullable = false)
    private Boolean activa = true;
    
    /**
     * Indica si la cuenta acepta movimientos directos.
     * False para cuentas de agrupación que solo tienen subcuentas.
     */
    @Column(nullable = false)
    private Boolean aceptaMovimientos = true;
    
    /**
     * Descripción adicional de la cuenta.
     */
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Column(length = 500)
    private String descripcion;
    
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
    
    // ==================== MÉTODOS DE NEGOCIO ====================
    
    /**
     * Callback ejecutado antes de persistir o actualizar la entidad.
     * Calcula el nivel jerárquico y valida la naturaleza de la cuenta.
     */
    @PrePersist
    @PreUpdate
    private void prepararEntidad() {
        // Calcular nivel jerárquico según el código
        if (codigo != null) {
            nivel = (int) codigo.chars().filter(ch -> ch == '.').count() + 1;
        }
        
        // Validar que la naturaleza corresponda al tipo de cuenta
        if (tipo != null && naturaleza == null) {
            naturaleza = NaturalezaCuenta.fromTipoCuenta(tipo);
        }
    }
    
    /**
     * Verifica si es una cuenta de agrupación (tiene subcuentas).
     * @return true si tiene subcuentas
     */
    public boolean esCuentaDeAgrupacion() {
        return subcuentas != null && !subcuentas.isEmpty();
    }
    
    /**
     * Verifica si puede registrarse un movimiento en esta cuenta.
     * @return true si está activa, acepta movimientos y no es de agrupación
     */
    public boolean puedeRecibirMovimientos() {
        return activa && aceptaMovimientos && !esCuentaDeAgrupacion();
    }
    
    /**
     * Obtiene el código completo de la jerarquía (incluyendo padres).
     * @return Código completo con formato legible
     */
    public String getCodigoCompleto() {
        if (cuentaPadre != null) {
            return cuentaPadre.getCodigoCompleto() + " > " + codigo + " - " + nombre;
        }
        return codigo + " - " + nombre;
    }
    
    /**
     * Agrega una subcuenta a esta cuenta.
     * @param subcuenta Cuenta hija a agregar
     */
    public void agregarSubcuenta(CuentaContable subcuenta) {
        subcuentas.add(subcuenta);
        subcuenta.setCuentaPadre(this);
        // Las cuentas con subcuentas no aceptan movimientos directos
        this.aceptaMovimientos = false;
    }
    
    /**
     * Elimina una subcuenta de esta cuenta.
     * @param subcuenta Cuenta hija a eliminar
     */
    public void eliminarSubcuenta(CuentaContable subcuenta) {
        subcuentas.remove(subcuenta);
        subcuenta.setCuentaPadre(null);
        // Si ya no tiene subcuentas, puede aceptar movimientos nuevamente
        if (subcuentas.isEmpty()) {
            this.aceptaMovimientos = true;
        }
    }
}
