package api.astro.whats_orders_manager.modules.rrhh.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Salario mínimo por categoría ocupacional (Decreto MTSS).
 * Updated semiannually (January and July). vigenciaHasta=null means indefinitely active.
 * Categories: TONC, TOSC, TOC, TES, TOE, DOM.
 */
@Entity
@Table(name = "salarios_minimos", indexes = {
    @Index(name = "idx_smin_categoria", columnList = "categoria"),
    @Index(name = "idx_smin_vigencia", columnList = "vigencia_desde, vigencia_hasta")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SalarioMinimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MTSS category code. Examples: TONC, TOSC, TOC, TES, TOE, DOM.
     */
    @Column(name = "categoria", nullable = false, length = 20)
    private String categoria;

    @Column(name = "descripcion_categoria", nullable = false, length = 200)
    private String descripcionCategoria;

    @Column(name = "monto_mensual", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoMensual;

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
