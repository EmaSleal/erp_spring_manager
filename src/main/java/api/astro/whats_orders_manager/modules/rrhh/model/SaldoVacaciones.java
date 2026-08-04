package api.astro.whats_orders_manager.modules.rrhh.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Saldo de vacaciones por empleado — Código de Trabajo CR Art. 153.
 * Un empleado acumula mínimo 2 semanas (10 días hábiles) por cada 50 semanas
 * de trabajo continuo.
 *
 * diasDisponibles is computed by the database as (dias_generados - dias_disfrutados).
 * The column is mapped read-only (insertable=false, updatable=false).
 */
@Entity
@Table(name = "saldo_vacaciones")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SaldoVacaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false, unique = true)
    private Empleado empleado;

    @Column(name = "dias_generados", nullable = false, precision = 6, scale = 2)
    private BigDecimal diasGenerados = BigDecimal.ZERO;

    @Column(name = "dias_disfrutados", nullable = false, precision = 6, scale = 2)
    private BigDecimal diasDisfrutados = BigDecimal.ZERO;

    /**
     * Computed by DB: dias_generados - dias_disfrutados.
     * Mapped as read-only to avoid Hibernate trying to write it.
     */
    @Column(name = "dias_disponibles", insertable = false, updatable = false, precision = 6, scale = 2)
    private BigDecimal diasDisponibles;

    @Column(name = "fecha_ultimo_calculo", nullable = false)
    private LocalDate fechaUltimoCalculo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Transient helper returning the current available days.
     * Uses in-memory calculation (diasGenerados - diasDisfrutados) so it is
     * always correct even before the DB computes the generated column.
     */
    public BigDecimal getDiasDisponiblesCalculados() {
        BigDecimal generados = diasGenerados != null ? diasGenerados : BigDecimal.ZERO;
        BigDecimal disfrutados = diasDisfrutados != null ? diasDisfrutados : BigDecimal.ZERO;
        return generados.subtract(disfrutados);
    }
}
