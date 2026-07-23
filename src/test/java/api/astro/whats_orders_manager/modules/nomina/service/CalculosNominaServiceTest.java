package api.astro.whats_orders_manager.modules.nomina.service;

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
 *   15 %   : [1347000, 2371000)
 *   20 %   : [2371000, 4740000)
 *   25 %   : [4740000, ∞)
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

        // ordered by limiteInferior ASC — caller guarantee
        tramos2026 = List.of(
                tramo("0",       "918000",  "0.00",  "1710", "2590"),
                tramo("918000",  "1347000", "0.10",  null,   null),
                tramo("1347000", "2371000", "0.15",  null,   null),
                tramo("2371000", "4740000", "0.20",  null,   null),
                tramo("4740000", null,      "0.25",  null,   null)
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
    // 2.2.3 — calcularIns
    // =========================================================================

    @Test
    @DisplayName("2.2.3 calcularIns: bruto=1500000 → 1500000 × 0.01 = 15000.00")
    void calcularIns_bruto1500000_returns15000() {
        BigDecimal bruto = new BigDecimal("1500000");

        BigDecimal result = service.calcularIns(bruto);

        assertEquals(new BigDecimal("15000.00"), result);
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
}
