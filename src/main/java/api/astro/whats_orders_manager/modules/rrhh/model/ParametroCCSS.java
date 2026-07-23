package api.astro.whats_orders_manager.modules.rrhh.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Parámetros de cotización CCSS vigentes para un período.
 * Supports versioned records: one record is active per date range.
 * vigenciaHasta=null means indefinitely active.
 */
@Entity
@Table(name = "parametros_ccss", indexes = {
    @Index(name = "idx_ccss_vigencia", columnList = "vigencia_desde, vigencia_hasta")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParametroCCSS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Total obrero: SEM + IVM + BP. Example 2026: 0.1083 */
    @Column(name = "porcentaje_obrero", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeObrero;

    /** Total patronal. Example 2026: 0.2683 */
    @Column(name = "porcentaje_patronal", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajePatronal;

    /** SEM obrero. Example 2026: 0.0550 */
    @Column(name = "porcentaje_sem", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeSem;

    /** IVM obrero. Example 2026: 0.0433 */
    @Column(name = "porcentaje_ivm_obrero", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeIvmObrero;

    /** Banco Popular obrero. Example 2026: 0.0100 */
    @Column(name = "porcentaje_bp_obrero", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeBpObrero;

    /** FCL patronal. Example 2026: 0.0150 */
    @Column(name = "porcentaje_fcl", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeFcl;

    /** ROP patronal. Example 2026: 0.0200 */
    @Column(name = "porcentaje_rop", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeRop;

    /** Base mínima contributiva SEM. Example 2026: ₡333,328 */
    @Column(name = "base_minima_contributiva_sem", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseMinimaContributivaSem;

    /** Base mínima contributiva IVM. Example 2026: ₡311,990 */
    @Column(name = "base_minima_contributiva_ivm", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseMinimaContributivaIvm;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    /** null means indefinitely active */
    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
