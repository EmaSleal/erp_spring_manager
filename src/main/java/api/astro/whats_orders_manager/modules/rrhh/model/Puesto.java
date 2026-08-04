package api.astro.whats_orders_manager.modules.rrhh.model;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Puesto de trabajo vinculado a un Departamento.
 * categoriaSalarialMinima is nullable — min-wage enforcement is wired in PR2.
 */
@Entity
@Table(name = "puestos", indexes = {
    @Index(name = "idx_puesto_departamento", columnList = "departamento_id"),
    @Index(name = "idx_puesto_activo", columnList = "activo")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Puesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "salario_base", nullable = false, precision = 19, scale = 2)
    private BigDecimal salarioBase = BigDecimal.ZERO;

    /**
     * Nullable — when null, the minimum-wage check is skipped.
     * Enforcement against SalarioMinimo will be wired in PR2.
     */
    @Column(name = "categoria_salarial_minima", length = 100)
    private String categoriaSalarialMinima;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_jornada", length = 20)
    private TipoJornada tipoJornada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
