package api.astro.whats_orders_manager.modules.nomina.model;

import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import jakarta.persistence.*;
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
 * Payroll header entity.
 *
 * Represents one payroll run for a given period and type (MENSUAL / QUINCENAL).
 * State machine is enforced via EstadoNomina helpers.
 * Audit fields mirror AsientoContable pattern (AuditingEntityListener).
 */
@Entity
@Table(
    name = "nominas",
    indexes = {
        @Index(name = "idx_nomina_estado", columnList = "estado"),
        @Index(name = "idx_nomina_numero", columnList = "numero", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"detalles"})
@EntityListeners(AuditingEntityListener.class)
public class Nomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Consecutive identifier, e.g. NOM-2026-0001. */
    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private LocalDate periodoFin;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoNomina tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoNomina estado = EstadoNomina.BORRADOR;

    // ── Aggregated totals (populated during calcular) ──────────────────────

    @Column(name = "total_bruto", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalBruto = BigDecimal.ZERO;

    @Column(name = "total_deducciones", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalDeducciones = BigDecimal.ZERO;

    @Column(name = "total_neto", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalNeto = BigDecimal.ZERO;

    @Column(name = "total_ccss_patronal", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalCcssPatronal = BigDecimal.ZERO;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    // ── Detail lines ───────────────────────────────────────────────────────

    @OneToMany(mappedBy = "nomina", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleNomina> detalles = new ArrayList<>();

    // ── Audit fields (same as AsientoContable) ─────────────────────────────

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Integer createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private Integer lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    // ── State machine guards (delegate to EstadoNomina) ───────────────────

    /** @return true if this payroll can receive a calculation run. */
    public boolean puedeCalcular() {
        return estado != null && estado.puedeCalcular();
    }

    /** @return true if this payroll can be approved. */
    public boolean puedeAprobar() {
        return estado != null && estado.puedeAprobar();
    }

    /** @return true if this payroll can be posted to accounting. */
    public boolean puedeContabilizar() {
        return estado != null && estado.puedeContabilizar();
    }

    /** @return true if this payroll can be cancelled. */
    public boolean puedeAnular() {
        return estado != null && estado.puedeAnular();
    }
}
