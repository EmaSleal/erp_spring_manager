package api.astro.whats_orders_manager.modules.nomina.model;

import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Payroll detail line for one employee within a payroll run.
 *
 * Each row holds gross-to-net calculation results. A UNIQUE constraint on
 * (nomina_id, empleado_id) ensures no employee appears twice in the same payroll.
 */
@Entity
@Table(
    name = "detalles_nomina",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_nomina_empleado", columnNames = {"nomina_id", "empleado_id"})
    },
    indexes = {
        @Index(name = "idx_detalle_nomina", columnList = "nomina_id"),
        @Index(name = "idx_detalle_empleado", columnList = "empleado_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"nomina", "empleado"})
public class DetalleNomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomina_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_detalle_nomina"))
    private Nomina nomina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_detalle_empleado"))
    private Empleado empleado;

    // ── Gross salary inputs ────────────────────────────────────────────────

    @Column(name = "salario_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal salarioBase;

    @Column(name = "horas_ausentes", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal horasAusentes = BigDecimal.ZERO;

    @Column(name = "bruto_prorrateado", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal brutoProrrateado = BigDecimal.ZERO;

    // ── Employee deductions ────────────────────────────────────────────────

    @Column(name = "ccss_obrero", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ccssObrero = BigDecimal.ZERO;

    @Column(name = "ins", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ins = BigDecimal.ZERO;

    @Column(name = "impuesto_renta", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal impuestoRenta = BigDecimal.ZERO;

    @Column(name = "credito_familiar", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal creditoFamiliar = BigDecimal.ZERO;

    @Column(name = "solidarista", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal solidarista = BigDecimal.ZERO;

    @Column(name = "pension_alimentaria", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal pensionAlimentaria = BigDecimal.ZERO;

    @Column(name = "otras_deducciones", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otrasDeducciones = BigDecimal.ZERO;

    // ── Summary ────────────────────────────────────────────────────────────

    @Column(name = "total_deducciones", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalDeducciones = BigDecimal.ZERO;

    @Column(name = "salario_neto", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal salarioNeto = BigDecimal.ZERO;

    // ── Employer cost ──────────────────────────────────────────────────────

    @Column(name = "ccss_patronal", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ccssPatronal = BigDecimal.ZERO;
}
