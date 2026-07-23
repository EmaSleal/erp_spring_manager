package api.astro.whats_orders_manager.modules.nomina.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Full gross-to-net breakdown for one employee line in a payroll run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleNominaDTO {

    private Long id;
    private Long empleadoId;
    private String empleadoNombre;

    // Gross inputs
    private BigDecimal salarioBase;
    private BigDecimal horasAusentes;
    private BigDecimal brutoProrrateado;

    // Employee deductions
    private BigDecimal ccssObrero;
    private BigDecimal ins;
    private BigDecimal impuestoRenta;
    private BigDecimal creditoFamiliar;
    private BigDecimal solidarista;
    private BigDecimal pensionAlimentaria;

    // Summary
    private BigDecimal totalDeducciones;
    private BigDecimal salarioNeto;

    // Employer cost (not deducted from employee)
    private BigDecimal ccssPatronal;
}
