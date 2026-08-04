package api.astro.whats_orders_manager.modules.rrhh.model;

import api.astro.whats_orders_manager.modules.rrhh.enums.EstadoCivil;
import api.astro.whats_orders_manager.modules.rrhh.enums.EstadoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.enums.Genero;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "empleados")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Datos de identidad (Art. 24 CT) ---
    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String primerApellido;

    @Column(length = 100)
    private String segundoApellido;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Genero genero;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoCivil estadoCivil;

    // --- Contacto ---
    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 15)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column
    private LocalDate fechaNacimiento;

    // --- Cargo ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;

    // --- Información laboral ---
    @Column(nullable = false)
    private LocalDate fechaIngreso;

    @Column
    private LocalDate fechaSalida;

    @Column(length = 500)
    private String motivoSalida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    @Column(nullable = false)
    private Boolean activo = true;

    // --- Campos normativos CR (SICERE / Nómina) ---
    @Column(name = "numero_asegurado_ccss", unique = true, length = 20)
    private String numeroAseguradoCCSS;

    @Column(name = "operadora_pensiones_rop", length = 100)
    private String operadoraPensionesROP;

    @Column(nullable = false)
    private Integer hijosCargaFamiliar = 0;

    @Column(nullable = false)
    private Boolean conyugeCargaFamiliar = false;

    @Column(precision = 5, scale = 4)
    private BigDecimal porcentajeSolidarista;

    @Column(precision = 19, scale = 2)
    private BigDecimal montoPensionAlimentaria;

    // --- Datos bancarios ---
    @Column(length = 30)
    private String cuentaBancaria;

    @Column(length = 100)
    private String banco;

    // --- Contacto de emergencia ---
    @Column(length = 200)
    private String contactoEmergenciaNombre;

    @Column(length = 15)
    private String contactoEmergenciaTelefono;

    @Column(length = 50)
    private String contactoEmergenciaRelacion;

    // --- Foto ---
    @Column(length = 500)
    private String fotoUrl;

    // --- Usuario del sistema (nullable — not every employee has an ERP account) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id_usuario", unique = true)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Helpers ---

    public String getNombreCompleto() {
        String completo = nombre + " " + primerApellido;
        if (segundoApellido != null && !segundoApellido.isBlank()) {
            completo += " " + segundoApellido;
        }
        return completo;
    }

    public int getAntiguedadAnios() {
        LocalDate fin = (fechaSalida != null) ? fechaSalida : LocalDate.now();
        return Period.between(fechaIngreso, fin).getYears();
    }
}
