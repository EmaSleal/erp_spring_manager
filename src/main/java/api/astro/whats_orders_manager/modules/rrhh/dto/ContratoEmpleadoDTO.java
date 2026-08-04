package api.astro.whats_orders_manager.modules.rrhh.dto;

import api.astro.whats_orders_manager.modules.rrhh.enums.CausaTerminacion;
import api.astro.whats_orders_manager.modules.rrhh.enums.FormaPago;
import api.astro.whats_orders_manager.modules.rrhh.enums.PeriodicidadPago;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoContrato;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ContratoEmpleadoDTO {

    private Long id;

    /** ID del empleado al que pertenece este contrato. */
    private Long empleadoId;

    private TipoContrato tipoContrato;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal salarioBruto;
    private TipoJornada jornada;
    private String cargoContratado;
    private String justificacionTemporalidad;
    private CausaTerminacion causaTerminacion;
    private LocalDate fechaTerminacion;
    private String descripcionTerminacion;
    private Boolean activo;

    // --- Campos normativos CR (V9) ---
    private String lugarTrabajo;
    private LocalDate fechaFinPeriodoPrueba;
    private Integer horasSemanales;
    private FormaPago formaPago;
    private PeriodicidadPago periodicidadPago;
}
