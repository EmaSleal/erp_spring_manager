package api.astro.whats_orders_manager.modules.nomina.service;

import api.astro.whats_orders_manager.modules.nomina.service.dto.ResultadoCalculoNomina;
import api.astro.whats_orders_manager.modules.rrhh.model.ParametroCCSS;
import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure, deterministic gross-to-net payroll calculation engine for Costa Rica.
 *
 * No DB access. All rates are injected by the caller (ParametroCCSS, TramoImpuestoSalario).
 * All monetary results use RoundingMode.HALF_UP with scale 2.
 *
 * Legal basis (2026):
 *   - CCSS: obrero 10.83 %, patronal 26.83 % (CCSS SICERE rates)
 *   - INS: occupational-risk tariff hardcoded at 1.00 % (not DB-modeled)
 *   - Income tax: marginal brackets from TramoImpuestoSalario, credits from exento row
 */
@Service
public class CalculosNominaService {

    /** Total ordinary hours per monthly pay period (convention). */
    private static final BigDecimal HORAS_PERIODO = new BigDecimal("200");

    /** INS occupational-risk tariff — legal constant, not DB-modeled. */
    private static final BigDecimal INS_RATE = new BigDecimal("0.0100");

    // ── Public calculation methods ────────────────────────────────────────────

    /**
     * Prorates the gross salary by reducing it proportionally for unpaid absent hours.
     *
     * @param salarioBase   monthly agreed salary
     * @param horasAusentes unpaid absent hours in the period (0 = full period worked)
     * @return brutoProrrateado = salarioBase × (HORAS_PERIODO - horasAusentes) / HORAS_PERIODO
     */
    public BigDecimal prorratearBruto(BigDecimal salarioBase, BigDecimal horasAusentes) {
        BigDecimal horasTrabajadas = HORAS_PERIODO.subtract(horasAusentes);
        return salarioBase
                .multiply(horasTrabajadas)
                .divide(HORAS_PERIODO, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the employee CCSS contribution (SEM + IVM + BP).
     *
     * @param bruto gross prorated salary
     * @param ccss  current CCSS parameters (rate is already decimal, e.g. 0.1083)
     * @return bruto × ccss.porcentajeObrero
     */
    public BigDecimal calcularCcssObrero(BigDecimal bruto, ParametroCCSS ccss) {
        return bruto
                .multiply(ccss.getPorcentajeObrero())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the INS occupational-risk contribution (1 % — hardcoded legal constant).
     *
     * @param bruto gross prorated salary
     * @return bruto × 0.0100
     */
    public BigDecimal calcularIns(BigDecimal bruto) {
        return bruto
                .multiply(INS_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Costa Rica income tax on salary using marginal brackets.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Locate the exento row (limiteInferior == 0) to read credits.</li>
     *   <li>Accumulate marginal tax across every bracket where baseGravable > limiteInferior.</li>
     *   <li>Subtract total credits (hijos × creditoPorHijo + conyuge × creditoPorConyuge).</li>
     *   <li>Floor result at ZERO — tax is never negative.</li>
     * </ol>
     *
     * @param baseGravable taxable base (brutoProrrateado - ccssObrero - ins)
     * @param tramos       tax brackets ordered by limiteInferior ASC (caller guarantees order)
     * @param hijos        number of dependent children
     * @param conyuge      whether the employee has a registered spouse credit
     * @return impuestoRenta ≥ 0
     */
    public BigDecimal calcularImpuestoRenta(
            BigDecimal baseGravable,
            List<TramoImpuestoSalario> tramos,
            int hijos,
            boolean conyuge) {

        // Locate exento row (limiteInferior == 0) to read tax credits
        TramoImpuestoSalario exento = tramos.stream()
                .filter(t -> t.getLimiteInferior().compareTo(BigDecimal.ZERO) == 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tax bracket list must contain an exento row (limiteInferior=0)"));

        BigDecimal creditoPorHijo = exento.getCreditoPorHijo() != null
                ? exento.getCreditoPorHijo() : BigDecimal.ZERO;
        BigDecimal creditoPorConyuge = exento.getCreditoPorConyuge() != null
                ? exento.getCreditoPorConyuge() : BigDecimal.ZERO;

        // Accumulate marginal tax
        BigDecimal rawTax = BigDecimal.ZERO;
        for (TramoImpuestoSalario tramo : tramos) {
            if (baseGravable.compareTo(tramo.getLimiteInferior()) <= 0) {
                // base does not reach this bracket's floor
                continue;
            }
            BigDecimal ceiling = tramo.getLimiteSuperior() != null
                    ? baseGravable.min(tramo.getLimiteSuperior())
                    : baseGravable;
            BigDecimal taxableInBracket = ceiling.subtract(tramo.getLimiteInferior());
            rawTax = rawTax.add(taxableInBracket.multiply(tramo.getPorcentaje()));
        }
        rawTax = rawTax.setScale(2, RoundingMode.HALF_UP);

        // Apply credits and floor at zero
        BigDecimal totalCreditos = creditoPorHijo.multiply(BigDecimal.valueOf(hijos))
                .add(conyuge ? creditoPorConyuge : BigDecimal.ZERO);

        BigDecimal impuesto = rawTax.subtract(totalCreditos);
        return impuesto.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the employer CCSS contribution.
     *
     * @param bruto gross prorated salary
     * @param ccss  current CCSS parameters (rate is already decimal, e.g. 0.2683)
     * @return bruto × ccss.porcentajePatronal
     */
    public BigDecimal calcularCargaPatronalCcss(BigDecimal bruto, ParametroCCSS ccss) {
        return bruto
                .multiply(ccss.getPorcentajePatronal())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Full gross-to-net calculation for one employee in one pay period.
     *
     * <p>Deduction order:
     * <ol>
     *   <li>Prorate bruto for absent hours.</li>
     *   <li>CCSS obrero + INS.</li>
     *   <li>baseGravable = brutoProrrateado - ccssObrero - ins.</li>
     *   <li>Income tax on baseGravable.</li>
     *   <li>Optional: solidarista (% of bruto) + pension alimentaria (fixed amount).</li>
     *   <li>totalDeducciones = ccssObrero + ins + impuestoRenta + solidarista + pensionAlimentaria.</li>
     *   <li>salarioNeto = brutoProrrateado - totalDeducciones.</li>
     *   <li>ccssPatronal (employer cost, not deducted from employee).</li>
     * </ol>
     *
     * @param salarioBase          monthly agreed salary
     * @param horasAusentes        unpaid absent hours (BigDecimal, may be ZERO)
     * @param porcentajeSolidarista optional solidarista pct (decimal, e.g. 0.03); null = no deduction
     * @param pensionAlimentaria    optional fixed alimony deduction; null = no deduction
     * @param ccss                 current CCSS parameters
     * @param tramos               income-tax brackets ordered by limiteInferior ASC
     * @param hijos                number of dependent children for tax credits
     * @param conyuge              whether the employee has a registered spouse credit
     * @return ResultadoCalculoNomina with all component breakdowns
     */
    public ResultadoCalculoNomina calcular(
            BigDecimal salarioBase,
            BigDecimal horasAusentes,
            BigDecimal porcentajeSolidarista,
            BigDecimal pensionAlimentaria,
            ParametroCCSS ccss,
            List<TramoImpuestoSalario> tramos,
            int hijos,
            boolean conyuge) {

        // 1. Prorated gross
        BigDecimal brutoProrrateado = prorratearBruto(salarioBase, horasAusentes);

        // 2. Employee CCSS and INS
        BigDecimal ccssObrero = calcularCcssObrero(brutoProrrateado, ccss);
        BigDecimal ins = calcularIns(brutoProrrateado);

        // 3. Taxable base
        BigDecimal baseGravable = brutoProrrateado
                .subtract(ccssObrero)
                .subtract(ins)
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Income tax and family credit
        BigDecimal impuestoRenta = calcularImpuestoRenta(baseGravable, tramos, hijos, conyuge);

        // Compute total credits for record transparency
        TramoImpuestoSalario exento = tramos.stream()
                .filter(t -> t.getLimiteInferior().compareTo(BigDecimal.ZERO) == 0)
                .findFirst()
                .orElseThrow();
        BigDecimal creditoPorHijo = exento.getCreditoPorHijo() != null
                ? exento.getCreditoPorHijo() : BigDecimal.ZERO;
        BigDecimal creditoPorConyuge = exento.getCreditoPorConyuge() != null
                ? exento.getCreditoPorConyuge() : BigDecimal.ZERO;
        BigDecimal creditoFamiliar = creditoPorHijo.multiply(BigDecimal.valueOf(hijos))
                .add(conyuge ? creditoPorConyuge : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 5. Optional deductions
        BigDecimal solidarista = porcentajeSolidarista != null
                ? brutoProrrateado.multiply(porcentajeSolidarista).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pension = pensionAlimentaria != null
                ? pensionAlimentaria.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 6. Total deductions (employee-side only)
        BigDecimal totalDeducciones = ccssObrero
                .add(ins)
                .add(impuestoRenta)
                .add(solidarista)
                .add(pension)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. Net salary
        BigDecimal salarioNeto = brutoProrrateado
                .subtract(totalDeducciones)
                .setScale(2, RoundingMode.HALF_UP);

        // 8. Employer cost (not deducted from employee)
        BigDecimal ccssPatronal = calcularCargaPatronalCcss(brutoProrrateado, ccss);

        return new ResultadoCalculoNomina(
                brutoProrrateado,
                ccssObrero,
                ins,
                baseGravable,
                impuestoRenta,
                creditoFamiliar,
                solidarista,
                pension,
                ccssPatronal,
                totalDeducciones,
                salarioNeto
        );
    }
}
