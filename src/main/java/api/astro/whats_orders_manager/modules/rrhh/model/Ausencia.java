package api.astro.whats_orders_manager.modules.rrhh.model;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoAusencia;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ausencias")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Ausencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "departamento", "puesto", "usuario", "contratos"})
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ausencia", nullable = false, length = 50)
    private TipoAusencia tipoAusencia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "con_goce_salario", nullable = false)
    private Boolean conGoceSalario = true;

    @Column(name = "computa_para_aguinaldo", nullable = false)
    private Boolean computaParaAguinaldo = true;

    @Column(name = "computa_antiguedad", nullable = false)
    private Boolean computaAntiguedad = true;

    @Column(name = "justificada", nullable = false)
    private Boolean justificada = true;

    @Column(name = "aprobada", nullable = false)
    private Boolean aprobada = false;

    // --- Campos normativos de certificación (V9) ---

    /**
     * Entidad que emite el certificado de incapacidad: "CCSS" o "INS".
     * Aplicable a INCAPACIDAD_CCSS e INCAPACIDAD_INS.
     */
    @Column(name = "entidad_certificante", length = 100)
    private String entidadCertificante;

    /** Número de boleta de incapacidad emitida por la entidad certificante. */
    @Column(name = "numero_boleta", length = 50)
    private String numeroBoleta;

    /**
     * Proporción del salario cubierta por el patrono durante la incapacidad.
     * Ej.: 0.50 = 50% (patrono paga los primeros 3 días al 100% o la parte no cubierta por CCSS).
     */
    @Column(name = "porcentaje_patrono", precision = 5, scale = 4)
    private BigDecimal porcentajePatrono;

    /**
     * Proporción del salario cubierta por el subsidio CCSS o INS.
     * Ej.: 0.60 = 60% (CCSS subsidia desde el 4° día al 60% del salario).
     */
    @Column(name = "porcentaje_subsidio", precision = 5, scale = 4)
    private BigDecimal porcentajeSubsidio;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobada_por", referencedColumnName = "id_usuario")
    private Usuario aprobadaPor;

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
}
