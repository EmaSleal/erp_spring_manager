package api.astro.whats_orders_manager.modules.rrhh.dto;

import api.astro.whats_orders_manager.modules.rrhh.enums.EstadoCivil;
import api.astro.whats_orders_manager.modules.rrhh.enums.EstadoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.enums.Genero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoDTO {

    // Read-only (server sets on response)
    private Long id;

    // Identity
    private String cedula;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;

    // Computed (read-only response field)
    private String nombreCompleto;

    // Contact
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Genero genero;
    private EstadoCivil estadoCivil;
    private String direccion;

    // Work assignment
    private Long departamentoId;
    private String departamentoNombre;
    private Long puestoId;
    private String puestoNombre;

    // Salary from puesto (read-only response)
    private BigDecimal salarioBase;

    // Labor info
    private LocalDate fechaIngreso;

    // Computed (read-only response)
    private Integer antiguedadAnios;

    private EstadoEmpleado estado;
    private Boolean activo;

    // CR regulatory fields
    private String numeroAseguradoCCSS;
    private String operadoraPensionesROP;
    private Integer hijosCargaFamiliar;
    private Boolean conyugeCargaFamiliar;
    private BigDecimal porcentajeSolidarista;
    private BigDecimal montoPensionAlimentaria;

    // Banking
    private String cuentaBancaria;
    private String banco;

    // Emergency contact
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String contactoEmergenciaRelacion;

    // Photo
    private String fotoUrl;

    // Optional user account link
    private Integer usuarioId;
}
