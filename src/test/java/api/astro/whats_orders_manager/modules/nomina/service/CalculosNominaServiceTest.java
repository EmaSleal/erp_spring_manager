package api.astro.whats_orders_manager.modules.nomina.service;

import api.astro.whats_orders_manager.modules.nomina.service.dto.ResultadoCalculoNomina;
import api.astro.whats_orders_manager.modules.rrhh.model.ParametroCCSS;
import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CalculosNominaService — pure calculation engine.
 *
 * RED phase — written before implementation.
 * No Spring context. No DB. All fixtures built in-test.
 *
 * Tax fixture: 2026 CR income-tax brackets.
 *   Exento : [0, 918000)   — 0 %,   creditoPorHijo=1710,  creditoPorConyuge=2590
 *   10 %   : [918000, 1347000)
 *   15 %   : [1347000, 2364000)
 *   20 %   : [2364000, 4727000)
 *   25 %   : [4727000, ∞)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalculosNominaService — unit tests")
class CalculosNominaServiceTest {

    private CalculosNominaService service;

    private ParametroCCSS ccss2026;
    private List<TramoImpuestoSalario> tramos2026;

    // ── Fixture builders ─────────────────────────────────────────────────────

    private static ParametroCCSS buildCcss2026() {
        ParametroCCSS c = new ParametroCCSS();
        c.setId(1L);
        c.setPorcentajeObrero(new BigDecimal("0.1083"));
        c.setPorcentajePatronal(new BigDecimal("0.2683"));
        c.setPorcentajeSem(new BigDecimal("0.0550"));
        c.setPorcentajeIvmObrero(new BigDecimal("0.0433"));
        c.setPorcentajeBpObrero(new BigDecimal("0.0100"));
        c.setPorcentajeFcl(new BigDecimal("0.0150"));
        c.setPorcentajeRop(new BigDecimal("0.0200"));
        c.setBaseMinimaContributivaSem(new BigDecimal("333328.00"));
        c.setBaseMinimaContributivaIvm(new BigDecimal("311990.00"));
        return c;
    }

    private static TramoImpuestoSalario tramo(
            String inferior, String superior, String pct,
            String hijo, String conyuge) {
        TramoImpuestoSalario t = new TramoImpuestoSalario();
        t.setAnioVigencia(2026);
        t.setLimiteInferior(new BigDecimal(inferior));
        t.setLimiteSuperior(superior != null ? new BigDecimal(superior) : null);
        t.setPorcentaje(new BigDecimal(pct));
        t.setCreditoPorHijo(hijo != null ? new BigDecimal(hijo) : null);
        t.setCreditoPorConyuge(conyuge != null ? new BigDecimal(conyuge) : null);
        return t;
    }

    @BeforeEach
    void setUp() {
        service = new CalculosNominaService();

        ccss2026 = buildCcss2026();

        // Same 2026 scale as V4__rrhh_catalogos_regulatorios.sql lines 55-59
        // ordered by limiteInferior ASC — caller guarantee
        tramos2026 = List.of(
                tramo("0",       "918000",  "0.00",  "1710", "2590"),
                tramo("918000",  "1347000", "0.10",  null,   null),
                tramo("1347000", "2364000", "0.15",  null,   null),
                tramo("2364000", "4727000", "0.20",  null,   null),
                tramo("4727000", null,      "0.25",  null,   null)
        );
    }

    // =========================================================================
    // 2.2.1 — prorratearBruto
    // =========================================================================

    @Test
    @DisplayName("2.2.1a prorratearBruto: 2 absent hours out of 200 → salarioBase × 198/200")
    void prorratearBruto_dosHorasAusentes_descuentaProporcional() {
        BigDecimal salarioBase = new BigDecimal("1000000");
        BigDecimal horasAusentes = new BigDecimal("2");

        BigDecimal result = service.prorratearBruto(salarioBase, horasAusentes);

        // 1000000 × 198/200 = 990000.00
        assertEquals(new BigDecimal("990000.00"), result);
    }

    @Test
    @DisplayName("2.2.1b prorratearBruto: 0 absent hours → full salarioBase unchanged")
    void prorratearBruto_sinAusencias_retornaBrutoCompleto() {
        BigDecimal salarioBase = new BigDecimal("1000000");
        BigDecimal horasAusentes = BigDecimal.ZERO;

        BigDecimal result = service.prorratearBruto(salarioBase, horasAusentes);

        assertEquals(new BigDecimal("1000000.00"), result);
    }

    // =========================================================================
    // 2.2.2 — calcularCcssObrero
    // =========================================================================

    @Test
    @DisplayName("2.2.2 calcularCcssObrero: rate=0.1083, bruto=918000 → 918000 × 0.1083")
    void calcularCcssObrero_bruto918000_rate1083() {
        BigDecimal bruto = new BigDecimal("918000");

        BigDecimal result = service.calcularCcssObrero(bruto, ccss2026);

        // 918000 × 0.1083 = 99419.40
        assertEquals(new BigDecimal("99419.40"), result);
    }

    // =========================================================================
    // 2.2.3 — calcularInsRiesgosPatronal (ADR-033)
    // =========================================================================

    @Test
    @DisplayName("2.2.3 calcularInsRiesgosPatronal: null rate → 0.00")
    void calcularInsRiesgosPatronal_tasaNull_returnsZero() {
        assertEquals(new BigDecimal("0.00"),
                service.calcularInsRiesgosPatronal(new BigDecimal("1500000"), null));
    }

    @Test
    @DisplayName("2.2.3 calcularInsRiesgosPatronal: rate 0 → 0.00")
    void calcularInsRiesgosPatronal_tasaCero_returnsZero() {
        assertEquals(new BigDecimal("0.00"),
                service.calcularInsRiesgosPatronal(new BigDecimal("1500000"), new BigDecimal("0.0000")));
    }

    @Test
    @DisplayName("2.2.3 calcularInsRiesgosPatronal: bruto=1500000 × 0.0125 = 18750.00")
    void calcularInsRiesgosPatronal_tasaConfigurada_brutoPorTasa() {
        assertEquals(new BigDecimal("18750.00"),
                service.calcularInsRiesgosPatronal(new BigDecimal("1500000"), new BigDecimal("0.0125")));
    }

    @Test
    @DisplayName("2.2.3 INS RT never reduces neto nor baseGravable (bruto 1 500 000)")
    void calcular_insRiesgos_noReduceNetoNiBaseGravable() {
        ResultadoCalculoNomina sinIns = service.calcular(new BigDecimal("1500000"), BigDecimal.ZERO,
                null, null, ccss2026, null, null, tramos2026, 0, false);
        ResultadoCalculoNomina conIns = service.calcular(new BigDecimal("1500000"), BigDecimal.ZERO,
                null, null, ccss2026, null, new BigDecimal("0.0125"), tramos2026, 0, false);

        assertEquals(new BigDecimal("0.00"), sinIns.insRiesgosPatronal());
        assertEquals(new BigDecimal("18750.00"), conIns.insRiesgosPatronal());
        assertEquals(sinIns.salarioNeto(), conIns.salarioNeto());
        assertEquals(sinIns.totalDeducciones(), conIns.totalDeducciones());
        // ADR-034: baseGravable = brutoProrrateado, CCSS obrera is not subtracted (1 500 000.00)
        assertEquals(new BigDecimal("1500000.00"), conIns.baseGravable());
        assertEquals(sinIns.baseGravable(), conIns.baseGravable());
    }

    // =========================================================================
    // 2.2.4 — income tax: base at or below exempt threshold → 0
    // =========================================================================

    @Test
    @DisplayName("2.2.4 calcularImpuestoRenta: baseGravable ≤ 918000 (exento) → 0.00")
    void calcularImpuestoRenta_baseExenta_retornaCero() {
        BigDecimal baseGravable = new BigDecimal("918000");

        BigDecimal result = service.calcularImpuestoRenta(baseGravable, tramos2026, 0, false);

        assertEquals(new BigDecimal("0.00"), result);
    }

    // =========================================================================
    // 2.2.5 — income tax: single bracket (10 %)
    // =========================================================================

    @Test
    @DisplayName("2.2.5 calcularImpuestoRenta: baseGravable=1000000 inside 10% bracket, no credits")
    void calcularImpuestoRenta_unTramo10pct_sinCreditos() {
        BigDecimal baseGravable = new BigDecimal("1000000");

        BigDecimal result = service.calcularImpuestoRenta(baseGravable, tramos2026, 0, false);

        // rawTax = (1000000 - 918000) × 0.10 = 82000 × 0.10 = 8200.00
        assertEquals(new BigDecimal("8200.00"), result);
    }

    // =========================================================================
    // 2.2.6 — credits clamp to zero (never negative)
    // =========================================================================

    @Test
    @DisplayName("2.2.6 calcularImpuestoRenta: credits exceed rawTax → clamped to 0.00")
    void calcularImpuestoRenta_creditosSuperanImpuesto_retornaCero() {
        // Use baseGravable that produces rawTax=500 (just above 918000 × 0.10 is too big;
        // craft a baseGravable where rawTax ≈ 500: 918000 + 5000 = 923000 → rawTax = 500)
        BigDecimal baseGravable = new BigDecimal("923000");
        // rawTax = (923000 - 918000) × 0.10 = 5000 × 0.10 = 500.00
        // 3 hijos: 3 × 1710 = 5130; cónyuge: 2590 → totalCreditos = 7720
        // max(0, 500 - 7720) = 0.00

        BigDecimal result = service.calcularImpuestoRenta(baseGravable, tramos2026, 3, true);

        assertEquals(new BigDecimal("0.00"), result);
    }

    // =========================================================================
    // 2.2.7 — CCSS patronal
    // =========================================================================

    @Test
    @DisplayName("2.2.7 calcularCargaPatronalCcss: rate=0.2683, bruto=1000000 → 268300.00")
    void calcularCargaPatronalCcss_bruto1000000_rate2683() {
        BigDecimal bruto = new BigDecimal("1000000");

        BigDecimal result = service.calcularCargaPatronalCcss(bruto, ccss2026);

        // 1000000 × 0.2683 = 268300.00
        assertEquals(new BigDecimal("268300.00"), result);
    }
    // =========================================================================
    // T-010 — aporte patronal ASE (ADR-013)
    // =========================================================================

    private static final BigDecimal TASA_ASE_2026 = new BigDecimal("0.0533");

    private ResultadoCalculoNomina calcular(String porcentajeSolidarista, String pension, BigDecimal tasaAse) {
        return service.calcular(
                new BigDecimal("1000000"),
                BigDecimal.ZERO,
                porcentajeSolidarista != null ? new BigDecimal(porcentajeSolidarista) : null,
                pension != null ? new BigDecimal(pension) : null,
                ccss2026,
                tasaAse,
                null,
                tramos2026,
                0,
                false);
    }

    @Test
    @DisplayName("T-010 aporte ASE = 0 when porcentajeSolidarista is null (not affiliated)")
    void calcular_solidaristaNull_aporteCero() {
        ResultadoCalculoNomina r = calcular(null, null, TASA_ASE_2026);
        assertEquals(new BigDecimal("0.00"), r.aportePatronalSolidarista());
    }

    @Test
    @DisplayName("T-010 aporte ASE = 0 when porcentajeSolidarista is 0 (not affiliated)")
    void calcular_solidaristaCero_aporteCero() {
        ResultadoCalculoNomina r = calcular("0.0000", null, TASA_ASE_2026);
        assertEquals(new BigDecimal("0.00"), r.aportePatronalSolidarista());
    }

    @Test
    @DisplayName("T-010 aporte ASE = 0 when the ASE rate is 0 % (allowed by ADR-013)")
    void calcular_tasaAseCero_aporteCero() {
        ResultadoCalculoNomina r = calcular("0.03", null, new BigDecimal("0.0000"));
        assertEquals(new BigDecimal("0.00"), r.aportePatronalSolidarista());
    }

    @Test
    @DisplayName("T-010 affiliated employee: aporte ASE = bruto × tasa (1 000 000 × 0.0533 = 53 300.00)")
    void calcular_afiliado_aporteBrutoPorTasa() {
        ResultadoCalculoNomina r = calcular("0.03", null, TASA_ASE_2026);
        assertEquals(new BigDecimal("53300.00"), r.aportePatronalSolidarista());
        assertEquals(new BigDecimal("30000.00"), r.solidarista());
    }

    @Test
    @DisplayName("T-010 aporte ASE uses the bruto prorrateado (2 absent hours → 990 000 × 0.0533)")
    void calcular_afiliadoConAusencias_aporteSobreBrutoProrrateado() {
        ResultadoCalculoNomina r = service.calcular(new BigDecimal("1000000"), new BigDecimal("2"),
                new BigDecimal("0.03"), null, ccss2026, TASA_ASE_2026, null, tramos2026, 0, false);
        assertEquals(new BigDecimal("990000.00"), r.brutoProrrateado());
        assertEquals(new BigDecimal("52767.00"), r.aportePatronalSolidarista());
    }

    @Test
    @DisplayName("T-010 salarioNeto and totalDeducciones unchanged by the ASE contribution")
    void calcular_aporteAse_noAlteraNetoNiDeducciones() {
        // ADR-034: base = 1 000 000; renta = 10% × (1 000 000 − 918 000) = 8 200
        // 1 000 000: ccssObrero 108 300 + renta 8 200
        //            + solidarista 30 000 + pensión 50 000 = 196 500 → neto 803 500
        ResultadoCalculoNomina conAse = calcular("0.03", "50000", TASA_ASE_2026);
        ResultadoCalculoNomina sinAse = calcular("0.03", "50000", BigDecimal.ZERO);

        assertEquals(new BigDecimal("196500.00"), conAse.totalDeducciones());
        assertEquals(new BigDecimal("803500.00"), conAse.salarioNeto());
        assertEquals(sinAse.totalDeducciones(), conAse.totalDeducciones());
        assertEquals(sinAse.salarioNeto(), conAse.salarioNeto());
        assertEquals(new BigDecimal("268300.00"), conAse.ccssPatronal());
        assertEquals(new BigDecimal("53300.00"), conAse.aportePatronalSolidarista());
    }

    @Test
    @DisplayName("T-010 vigencia: June rate 0.0533 → 53 300.00; July rate 0.0400 → 40 000.00")
    void calcular_tasaPorVigencia_juniOYJulio() {
        assertEquals(new BigDecimal("53300.00"),
                calcular("0.03", null, new BigDecimal("0.0533")).aportePatronalSolidarista());
        assertEquals(new BigDecimal("40000.00"),
                calcular("0.03", null, new BigDecimal("0.0400")).aportePatronalSolidarista());
    }

    // =========================================================================
    // T-028 — income tax scale applied on brutoProrrateado (ADR-034)
    // =========================================================================

    private ResultadoCalculoNomina calcularRenta(String bruto, int hijos, boolean conyuge) {
        return service.calcular(new BigDecimal(bruto), BigDecimal.ZERO, null, null,
                ccss2026, null, null, tramos2026, hijos, conyuge);
    }

    @Test
    @DisplayName("T-028a bruto 1 500 000, no credits → baseGravable 1 500 000.00, renta 65 850.00")
    void calcular_t028a_baseEsBrutoSinRestarCcss() {
        ResultadoCalculoNomina r = calcularRenta("1500000", 0, false);
        // 10% × (1 347 000 − 918 000) = 42 900 ; 15% × (1 500 000 − 1 347 000) = 22 950 ; total 65 850
        assertEquals(new BigDecimal("1500000.00"), r.baseGravable());
        assertEquals(new BigDecimal("65850.00"), r.impuestoRenta());
    }

    @Test
    @DisplayName("T-028b bruto 1 000 000, 1 child + spouse → 8 200 − 1 710 − 2 590 = 3 900.00")
    void calcular_t028b_creditosSeRestanDelImpuesto() {
        ResultadoCalculoNomina r = calcularRenta("1000000", 1, true);
        // 10% × (1 000 000 − 918 000) = 8 200 ; credits 1 710 + 2 590 = 4 300 ; 8 200 − 4 300 = 3 900
        assertEquals(new BigDecimal("1000000.00"), r.baseGravable());
        assertEquals(new BigDecimal("3900.00"), r.impuestoRenta());
    }

    @Test
    @DisplayName("T-028c bruto 918 000 → renta 0.00")
    void calcular_t028c_brutoExento() {
        // base 918 000 is not above the exempt threshold (918 000) → no taxable excess → 0
        assertEquals(new BigDecimal("0.00"), calcularRenta("918000", 0, false).impuestoRenta());
    }

    @Test
    @DisplayName("T-028d bruto 1 800 000, no credits → renta 110 850.00")
    void calcular_t028d_tramo15() {
        // 42 900 + 15% × (1 800 000 − 1 347 000) = 42 900 + 67 950 = 110 850
        assertEquals(new BigDecimal("110850.00"), calcularRenta("1800000", 0, false).impuestoRenta());
    }

    @Test
    @DisplayName("T-028e CCSS obrera still reduces the neto: 1 500 000 − 162 450 − 65 850 = 1 271 700.00")
    void calcular_t028e_ccssSigueRebajadaDelNeto() {
        ResultadoCalculoNomina r = calcularRenta("1500000", 0, false);
        // ccssObrero = 1 500 000 × 0.1083 = 162 450 ; neto = 1 500 000 − 162 450 − 65 850 = 1 271 700
        assertEquals(new BigDecimal("162450.00"), r.ccssObrero());
        assertEquals(new BigDecimal("228300.00"), r.totalDeducciones());
        assertEquals(new BigDecimal("1271700.00"), r.salarioNeto());
    }
}
