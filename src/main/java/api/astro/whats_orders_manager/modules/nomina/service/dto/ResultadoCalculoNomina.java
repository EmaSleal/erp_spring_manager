package api.astro.whats_orders_manager.modules.nomina.service.dto;

import java.math.BigDecimal;

/**
 * Immutable result of a gross-to-net payroll calculation for one employee period.
 * All monetary values are in Costa Rican colones with scale 2.
 * ccssPatronal and aportePatronalSolidarista are employer costs: they are not part of
 * totalDeducciones and do not reduce salarioNeto.
 */
public record ResultadoCalculoNomina(
        BigDecimal brutoProrrateado,
        BigDecimal ccssObrero,
        BigDecimal ins,
        BigDecimal baseGravable,
        BigDecimal impuestoRenta,
        BigDecimal creditoFamiliar,
        BigDecimal solidarista,
        BigDecimal pensionAlimentaria,
        BigDecimal ccssPatronal,
        BigDecimal aportePatronalSolidarista,
        BigDecimal totalDeducciones,
        BigDecimal salarioNeto
) {}
