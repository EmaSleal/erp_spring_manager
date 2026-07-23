package api.astro.whats_orders_manager.modules.rrhh.model;

import api.astro.whats_orders_manager.modules.rrhh.enums.CausaTerminacion;
import api.astro.whats_orders_manager.modules.rrhh.enums.FormaPago;
import api.astro.whats_orders_manager.modules.rrhh.enums.PeriodicidadPago;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoContrato;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contratos_empleado")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ContratoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false, length = 50)
    private TipoContrato tipoContrato;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "salario_bruto", nullable = false, precision = 15, scale = 2)
    private BigDecimal salarioBruto;

    @Enumerated(EnumType.STRING)
    @Column(name = "jornada", length = 50)
    private TipoJornada jornada;

    @Column(name = "cargo_contratado", length = 150)
    private String cargoContratado;

    @Column(name = "justificacion_temporalidad", length = 300)
    private String justificacionTemporalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "causa_terminacion", length = 80)
    private CausaTerminacion causaTerminacion;

    @Column(name = "fecha_terminacion")
    private LocalDate fechaTerminacion;

    @Column(name = "descripcion_terminacion", columnDefinition = "TEXT")
    private String descripcionTerminacion;

    // --- Campos normativos CR (V9) ---

    /** Lugar donde se prestan los servicios — Art. 24 inc. g CT. */
    @Column(name = "lugar_trabajo", length = 200)
    private String lugarTrabajo;

    /** Fecha en que concluye el período de prueba — Art. 28 CT (máximo 3 meses). */
    @Column(name = "fecha_fin_periodo_prueba")
    private LocalDate fechaFinPeriodoPrueba;

    /** Horas semanales pactadas — Art. 136 CT (jornada ordinaria máx. 48 h/semana). */
    @Column(name = "horas_semanales")
    private Integer horasSemanales;

    /** Forma de retribución del salario — Art. 163 CT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", length = 50)
    private FormaPago formaPago;

    /** Periodicidad con la que se paga el salario — Art. 164 CT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "periodicidad_pago", length = 50)
    private PeriodicidadPago periodicidadPago;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

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
