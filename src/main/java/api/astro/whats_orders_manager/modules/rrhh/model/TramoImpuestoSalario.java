package api.astro.whats_orders_manager.modules.rrhh.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tramo de impuesto al salario (renta) para un año de vigencia.
 * Brackets are ordered by limiteInferior ASC per anioVigencia.
 * Tax credits (creditoPorHijo, creditoPorConyuge) are stored on the exento row (limiteInferior=0).
 */
@Entity
@Table(name = "tramos_impuesto_salario", indexes = {
    @Index(name = "idx_tramo_anio", columnList = "anio_vigencia")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TramoImpuestoSalario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anio_vigencia", nullable = false)
    private Integer anioVigencia;

    /** Lower bound of bracket. 0 = exento (exempt bracket). */
    @Column(name = "limite_inferior", nullable = false, precision = 19, scale = 2)
    private BigDecimal limiteInferior;

    /** Upper bound of bracket. null = no upper limit (top bracket). */
    @Column(name = "limite_superior", precision = 19, scale = 2)
    private BigDecimal limiteSuperior;

    /** Tax rate for this bracket. 0.00 for exempt, 0.10 / 0.15 / 0.20 / 0.25 for others. */
    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentaje;

    /**
     * Monthly tax credit per dependent child.
     * Stored only on the exento row (limiteInferior=0). null on all other brackets.
     * Example 2026: ₡1,710
     */
    @Column(name = "credito_por_hijo", precision = 19, scale = 2)
    private BigDecimal creditoPorHijo;

    /**
     * Monthly tax credit for registered spouse.
     * Stored only on the exento row (limiteInferior=0). null on all other brackets.
     * Example 2026: ₡2,590
     */
    @Column(name = "credito_por_conyuge", precision = 19, scale = 2)
    private BigDecimal creditoPorConyuge;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
