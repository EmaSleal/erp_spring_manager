package api.astro.whats_orders_manager.modules.rrhh.dto;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoAusencia;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AusenciaDTO {

    private Long id;

    /** ID del empleado al que pertenece esta ausencia. */
    private Long empleadoId;

    private TipoAusencia tipoAusencia;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String descripcion;
    private Boolean conGoceSalario;
    private Boolean computaParaAguinaldo;
    private Boolean computaAntiguedad;
    private Boolean justificada;
    private Boolean aprobada;

    /** ID del usuario que aprueba la ausencia (nullable). */
    private Integer aprobadaPorId;

    // --- Campos normativos CR (V9) ---
    private String entidadCertificante;
    private String numeroBoleta;
    private BigDecimal porcentajePatrono;
    private BigDecimal porcentajeSubsidio;
}
