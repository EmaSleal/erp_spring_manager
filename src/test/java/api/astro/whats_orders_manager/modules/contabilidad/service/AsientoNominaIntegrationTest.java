package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.model.DetalleNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD — PR4: generarAsientoNomina tests.
 *
 * RED phase written before generarAsientoNomina exists in AsientoContableService.
 * Uses Mockito (no Spring context), consistent with existing service test patterns.
 *
 * Scenarios:
 * 4.2.1 — Missing NOMINA_SALARIOS_POR_PAGAR key → IllegalStateException
 * 4.2.2 — Entry is balanced (sum DR == sum CR)
 * 4.2.3 — Persisted asiento links back to nomina
 * 4.2.4 — generarAsientoNomina transitions asiento to CONTABILIZADO (and NominaService sets CONTABILIZADA)
 *
 * T-010 (ADR-013):
 * - base case without optional deductions: balanced, no solidarista/pensión/ASE lines
 * - solidarista 3 % + ASE employer contribution 5.33 %
 * - fixed pensión alimentaria without solidarista
 * - 2+ employees with both deductions: per-account totals are sums of the lines
 * - forced imbalance: message includes Debe, Haber and the difference
 *
 * Fixture (corrected in T-010): totalNeto = bruto − ccssObrero − ins − renta − solidarista − pensión
 * (881 700.00 for the base case) and NOMINA_CCSS_PATRONAL_POR_PAGAR maps to a liability account.
 * Previously the fixture inflated totalNeto to 1 150 000.00 (= 881 700 + 268 300, crediting the
 * CCSS patronal twice) and mapped the CCSS patronal liability key to an expense account (5.1.02).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsientoContableService.generarAsientoNomina — PR4 tests")
class AsientoNominaIntegrationTest {

    @Mock
    private AsientoContableRepository asientoRepository;

    @Mock
    private DetalleAsientoRepository detalleRepository;

    @Mock
    private CuentaContableRepository cuentaRepository;

    @Mock
    private ParametroContableRepository parametroContableRepository;

    private AsientoContableService service;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static final LocalDate INICIO = LocalDate.of(2026, 7, 1);
    private static final LocalDate FIN    = LocalDate.of(2026, 7, 31);

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(
                asientoRepository,
                detalleRepository,
                cuentaRepository,
                parametroContableRepository
        );
    }

    // =========================================================================
    // 4.2.1 — Missing NOMINA_SALARIOS_POR_PAGAR → IllegalStateException
    // =========================================================================

    @Test
    @DisplayName("4.2.1 generarAsientoNomina throws IllegalStateException when NOMINA_SALARIOS_POR_PAGAR is not configured")
    void generarAsientoNomina_missingSalariosPorPagarKey_throwsIllegalState() {
        // GIVEN: the other 6 base keys are present, NOMINA_SALARIOS_POR_PAGAR is absent
        CuentaContable cuenta = buildCuenta(1L, "1.1.01");

        when(parametroContableRepository.findByClave("NOMINA_GASTO_SUELDOS"))
                .thenReturn(Optional.of(buildParam("NOMINA_GASTO_SUELDOS", cuenta)));
        when(parametroContableRepository.findByClave("NOMINA_GASTO_CARGAS_SOCIALES"))
                .thenReturn(Optional.of(buildParam("NOMINA_GASTO_CARGAS_SOCIALES", cuenta)));
        when(parametroContableRepository.findByClave("NOMINA_CCSS_PATRONAL_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_CCSS_PATRONAL_POR_PAGAR", cuenta)));
        when(parametroContableRepository.findByClave("NOMINA_CCSS_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_CCSS_POR_PAGAR", cuenta)));
        when(parametroContableRepository.findByClave("NOMINA_INS_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_INS_POR_PAGAR", cuenta)));
        when(parametroContableRepository.findByClave("NOMINA_RENTA_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_RENTA_POR_PAGAR", cuenta)));
        // NOMINA_SALARIOS_POR_PAGAR absent:
        when(parametroContableRepository.findByClave("NOMINA_SALARIOS_POR_PAGAR"))
                .thenReturn(Optional.empty());

        Nomina nomina = buildNomina();

        // WHEN / THEN
        assertThatThrownBy(() -> service.generarAsientoNomina(nomina))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOMINA_SALARIOS_POR_PAGAR");

        verify(asientoRepository, never()).save(any());
    }

    // =========================================================================
    // 4.2.2 — Entry is balanced (DR == CR)
    // =========================================================================

    @Test
    @DisplayName("4.2.2 generarAsientoNomina produces a balanced journal entry (totalDebe == totalHaber)")
    void generarAsientoNomina_producesBalancedEntry() {
        // GIVEN: all 6 keys configured; each key maps to a distinct account
        seedAllKeys();

        Nomina nomina = buildNomina();
        // DR = bruto 1_000_000 + CCSS patronal 268_300 = 1_268_300
        // CR = ccssObrero 108_300 + ins 10_000 + renta 0 + neto 881_700 + CCSS patronal 268_300 = 1_268_300

        // save returns the same asiento (with id set)
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> {
            AsientoContable a = inv.getArgument(0);
            // simulate id assignment on first save
            return a;
        });

        // WHEN
        AsientoContable result = service.generarAsientoNomina(nomina);

        // THEN: must be balanced
        assertThat(result.getTotalDebe()).isEqualByComparingTo(result.getTotalHaber());
        assertThat(result.getTotalDebe()).isEqualByComparingTo("1268300.00");
    }

    // =========================================================================
    // 4.2.3 — Persisted asiento links back to nomina
    // =========================================================================

    @Test
    @DisplayName("4.2.3 generarAsientoNomina sets asiento.nomina to the originating Nomina")
    void generarAsientoNomina_linksAsientoToNomina() {
        seedAllKeys();

        Nomina nomina = buildNomina();

        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        when(asientoRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        service.generarAsientoNomina(nomina);

        // THEN: first save call should have nomina set
        AsientoContable captured = captor.getAllValues().get(0);
        assertThat(captured.getNomina()).isSameAs(nomina);
    }

    // =========================================================================
    // 4.2.4 — generarAsientoNomina transitions asiento estado to CONTABILIZADO
    // =========================================================================

    @Test
    @DisplayName("4.2.4 generarAsientoNomina returns asiento in CONTABILIZADO estado")
    void generarAsientoNomina_asientoIsContabilizado() {
        seedAllKeys();

        Nomina nomina = buildNomina();

        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        AsientoContable result = service.generarAsientoNomina(nomina);

        // THEN
        assertThat(result.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(result.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_NOMINA);
    }

    // =========================================================================
    // T-010 — solidarista, pensión alimentaria and ASE employer contribution (ADR-013)
    // =========================================================================

    @Test
    @DisplayName("T-010 base case without optional deductions: balanced, no solidarista/pensión/ASE lines")
    void generarAsientoNomina_sinDeduccionesOpcionales_cuadraSinLineasOpcionales() {
        seedAllKeys();
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        AsientoContable result = service.generarAsientoNomina(buildNomina());

        assertThat(result.getTotalDebe()).isEqualByComparingTo(result.getTotalHaber());
        assertThat(result.getDetalles()).hasSize(6);
        assertThat(result.getDetalles()).extracting(d -> d.getCuenta().getIdCuenta())
                .doesNotContain(C_SOLIDARISTA.getIdCuenta(), C_PENSION.getIdCuenta(), C_ASE_GASTO.getIdCuenta());
        assertThat(haber(result, C_SALARIOS)).isEqualByComparingTo("881700.00");
        assertThat(haber(result, C_CCSS_PATRONAL)).isEqualByComparingTo("268300.00");
        verify(parametroContableRepository, never()).findByClave("NOMINA_SOLIDARISTA_POR_PAGAR");
        verify(parametroContableRepository, never()).findByClave("NOMINA_PENSION_ALIMENTARIA_POR_PAGAR");
        verify(parametroContableRepository, never()).findByClave("NOMINA_APORTE_PATRONAL_ASE_GASTO");
        verify(parametroContableRepository, never()).findByClave("NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR");
    }

    @Test
    @DisplayName("T-010 solidarista 3 % on 1 000 000.00 with ASE rate 0.0533: CR solidarista 30 000.00, DR/CR ASE 53 300.00")
    void generarAsientoNomina_conSolidarista_acreditaAhorroYRegistraAportePatronal() {
        seedAllKeys();
        seedSolidaristaKeys();
        seedAseKeys();
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        // neto = 1_000_000 − 108_300 − 10_000 − 0 − 30_000 = 851_700
        Nomina nomina = buildNominaConLineas(
                linea("1000000.00", "108300.00", "10000.00", "0", "30000.00", "0", "268300.00", "53300.00"));

        AsientoContable result = service.generarAsientoNomina(nomina);

        assertThat(haber(result, C_SOLIDARISTA)).isEqualByComparingTo("83300.00"); // 30 000 obrero + 53 300 patronal
        assertThat(lineas(result, C_SOLIDARISTA, false)).extracting(DetalleAsiento::getHaber)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("30000.00"), new BigDecimal("53300.00"));
        assertThat(debe(result, C_ASE_GASTO)).isEqualByComparingTo("53300.00");
        assertThat(haber(result, C_SALARIOS)).isEqualByComparingTo("851700.00");
        assertThat(result.getDetalles()).extracting(d -> d.getCuenta().getIdCuenta())
                .doesNotContain(C_PENSION.getIdCuenta());
        assertThat(result.getTotalDebe()).isEqualByComparingTo(result.getTotalHaber());
        assertThat(result.getTotalDebe()).isEqualByComparingTo("1321600.00"); // 1 000 000 + 268 300 + 53 300
    }

    @Test
    @DisplayName("T-010 fixed pensión 50 000.00 without solidarista: CR pensión 50 000.00, no ASE lines")
    void generarAsientoNomina_conPension_acreditaPensionSinAse() {
        seedAllKeys();
        seedPensionKey();
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        // neto = 1_000_000 − 108_300 − 10_000 − 0 − 50_000 = 831_700
        Nomina nomina = buildNominaConLineas(
                linea("1000000.00", "108300.00", "10000.00", "0", "0", "50000.00", "268300.00", "0"));

        AsientoContable result = service.generarAsientoNomina(nomina);

        assertThat(haber(result, C_PENSION)).isEqualByComparingTo("50000.00");
        assertThat(haber(result, C_SALARIOS)).isEqualByComparingTo("831700.00");
        assertThat(result.getDetalles()).extracting(d -> d.getCuenta().getIdCuenta())
                .doesNotContain(C_SOLIDARISTA.getIdCuenta(), C_ASE_GASTO.getIdCuenta());
        assertThat(result.getTotalDebe()).isEqualByComparingTo(result.getTotalHaber());
        verify(parametroContableRepository, never()).findByClave("NOMINA_APORTE_PATRONAL_ASE_GASTO");
        verify(parametroContableRepository, never()).findByClave("NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR");
    }

    @Test
    @DisplayName("T-010 two+ employees with both deductions: per-account totals are sums of the lines and entry balances")
    void generarAsientoNomina_variosEmpleadosConAmbasDeducciones_totalesPorCuentaYCuadre() {
        seedAllKeys();
        seedSolidaristaKeys();
        seedPensionKey();
        seedAseKeys();
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        // Employee A: bruto 1_000_000, solidarista 3 % = 30_000, pensión 50_000, ASE 53_300
        //   neto = 1_000_000 − 108_300 − 10_000 − 0 − 30_000 − 50_000 = 801_700
        // Employee B: bruto 1_500_000, renta 17_870.50, solidarista 5 % = 75_000, pensión 120_000, ASE 79_950
        //   neto = 1_500_000 − 162_450 − 15_000 − 17_870.50 − 75_000 − 120_000 = 1_109_679.50
        // Employee C: bruto 600_000, no optional deductions
        //   neto = 600_000 − 64_980 − 6_000 = 529_020
        Nomina nomina = buildNominaConLineas(
                linea("1000000.00", "108300.00", "10000.00", "0", "30000.00", "50000.00", "268300.00", "53300.00"),
                linea("1500000.00", "162450.00", "15000.00", "17870.50", "75000.00", "120000.00", "402450.00", "79950.00"),
                linea("600000.00", "64980.00", "6000.00", "0", "0", "0", "160980.00", "0"));

        AsientoContable result = service.generarAsientoNomina(nomina);

        // ADR-017: the employer CCSS charge (831_730) is no longer debited to salaries
        assertThat(debe(result, C_GASTO_SUELDOS)).isEqualByComparingTo("3100000.00");
        assertThat(debe(result, C_CARGAS_SOCIALES)).isEqualByComparingTo("831730.00");
        assertThat(debe(result, C_ASE_GASTO)).isEqualByComparingTo("133250.00");      // 53_300 + 79_950
        assertThat(lineas(result, C_SOLIDARISTA, false)).extracting(DetalleAsiento::getHaber)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("105000.00"), new BigDecimal("133250.00"));
        assertThat(haber(result, C_PENSION)).isEqualByComparingTo("170000.00");       // 50_000 + 120_000
        assertThat(haber(result, C_SALARIOS)).isEqualByComparingTo("2440399.50");
        assertThat(haber(result, C_CCSS_PATRONAL)).isEqualByComparingTo("831730.00");
        assertThat(result.getTotalDebe()).isEqualByComparingTo(result.getTotalHaber());
        assertThat(result.getTotalDebe()).isEqualByComparingTo("4064980.00");
    }

    @Test
    @DisplayName("T-010 forced imbalance: exception message includes Debe, Haber and the difference")
    void generarAsientoNomina_descuadre_mensajeConDebeHaberYDiferencia() {
        seedAllKeys();

        Nomina nomina = buildNomina();
        nomina.setTotalNeto(new BigDecimal("882000.00")); // 300.00 more than the correct net

        assertThatThrownBy(() -> service.generarAsientoNomina(nomina))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Debe: 1268300.00")
                .hasMessageContaining("Haber: 1268600.00")
                .hasMessageContaining("diferencia: -300.00");

        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-010 solidarista present but NOMINA_SOLIDARISTA_POR_PAGAR not configured → IllegalStateException naming the key")
    void generarAsientoNomina_solidaristaSinClave_lanzaIllegalState() {
        seedBaseKeys();
        when(parametroContableRepository.findByClave("NOMINA_SOLIDARISTA_POR_PAGAR")).thenReturn(Optional.empty());

        Nomina nomina = buildNominaConLineas(
                linea("1000000.00", "108300.00", "10000.00", "0", "30000.00", "0", "268300.00", "0"));

        assertThatThrownBy(() -> service.generarAsientoNomina(nomina))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOMINA_SOLIDARISTA_POR_PAGAR");

        verify(asientoRepository, never()).save(any());
    }

    // =========================================================================
    // T-011 — entry date = nomina.periodoFin (ADR-014)
    // =========================================================================

    private static final LocalDate SEPT_INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEPT_FIN    = LocalDate.of(2026, 9, 30);

    @Test
    @DisplayName("T-011 the payroll entry is dated with periodoFin (2026-09-30), not with the system date")
    void generarAsientoNomina_fechaEsPeriodoFin() {
        seedAllKeys();
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        AsientoContable result = service.generarAsientoNomina(nominaSeptiembre());

        assertThat(result.getFecha()).isEqualTo(SEPT_FIN);
    }

    @Test
    @DisplayName("T-011 nomina without periodoFin → IllegalStateException naming the nomina; nothing saved")
    void generarAsientoNomina_sinPeriodoFin_lanzaIllegalState() {
        Nomina nomina = nominaSeptiembre();
        nomina.setPeriodoFin(null);

        assertThatThrownBy(() -> service.generarAsientoNomina(nomina))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOM-2026-0001")
                .hasMessageContaining("periodoFin");

        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-011 reversing the payroll entry: the reversal is dated today, the original keeps periodoFin")
    void generarAsientoReversion_deNomina_fechaDeAnulacion() {
        seedAllKeys();
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
        AsientoContable original = service.generarAsientoNomina(nominaSeptiembre());
        original.setIdAsiento(900L);

        LocalDate antes = LocalDate.now();
        AsientoContable reversion = service.generarAsientoReversion(original, "Nómina anulada");
        LocalDate despues = LocalDate.now();

        assertThat(reversion.getFecha()).isBetween(antes, despues);
        assertThat(reversion.getAsientoRevertido()).isSameAs(original);
        assertThat(original.getFecha()).isEqualTo(SEPT_FIN);
    }

    /** buildNomina() moved to September 2026 (periodoInicio 2026-09-01, periodoFin 2026-09-30). */
    private Nomina nominaSeptiembre() {
        Nomina nomina = buildNomina();
        nomina.setPeriodoInicio(SEPT_INICIO);
        nomina.setPeriodoFin(SEPT_FIN);
        nomina.setFechaPago(SEPT_FIN.plusDays(5));
        return nomina;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static final CuentaContable C_GASTO_SUELDOS = cuenta(10L, "5.1.01.001");
    private static final CuentaContable C_CARGAS_SOCIALES = cuenta(19L, "5.1.02.003");
    private static final CuentaContable C_CCSS_PATRONAL = cuenta(11L, "2.1.01.002");
    private static final CuentaContable C_SALARIOS      = cuenta(15L, "2.1.01.001");
    private static final CuentaContable C_SOLIDARISTA   = cuenta(16L, "2.1.04.001");
    private static final CuentaContable C_PENSION       = cuenta(17L, "2.1.04.002");
    private static final CuentaContable C_ASE_GASTO     = cuenta(18L, "5.1.02.001");

    private static CuentaContable cuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return c;
    }

    private void seedSolidaristaKeys() {
        when(parametroContableRepository.findByClave("NOMINA_SOLIDARISTA_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_SOLIDARISTA_POR_PAGAR", C_SOLIDARISTA)));
    }

    private void seedPensionKey() {
        when(parametroContableRepository.findByClave("NOMINA_PENSION_ALIMENTARIA_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_PENSION_ALIMENTARIA_POR_PAGAR", C_PENSION)));
    }

    /** ASE expense key + ASE liability key mapped to the SAME account as NOMINA_SOLIDARISTA_POR_PAGAR. */
    private void seedAseKeys() {
        when(parametroContableRepository.findByClave("NOMINA_APORTE_PATRONAL_ASE_GASTO"))
                .thenReturn(Optional.of(buildParam("NOMINA_APORTE_PATRONAL_ASE_GASTO", C_ASE_GASTO)));
        when(parametroContableRepository.findByClave("NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR", C_SOLIDARISTA)));
    }

    /** One payroll line; neto is derived as bruto − ccssObrero − ins − renta − solidarista − pensión. */
    private static DetalleNomina linea(String bruto, String ccssObrero, String ins, String renta,
                                       String solidarista, String pension, String ccssPatronal, String aporteAse) {
        DetalleNomina d = new DetalleNomina();
        d.setBrutoProrrateado(new BigDecimal(bruto));
        d.setCcssObrero(new BigDecimal(ccssObrero));
        d.setIns(new BigDecimal(ins));
        d.setImpuestoRenta(new BigDecimal(renta));
        d.setSolidarista(new BigDecimal(solidarista));
        d.setPensionAlimentaria(new BigDecimal(pension));
        d.setCcssPatronal(new BigDecimal(ccssPatronal));
        d.setAportePatronalSolidarista(new BigDecimal(aporteAse));
        BigDecimal deducciones = d.getCcssObrero().add(d.getIns()).add(d.getImpuestoRenta())
                .add(d.getSolidarista()).add(d.getPensionAlimentaria());
        d.setTotalDeducciones(deducciones);
        d.setSalarioNeto(d.getBrutoProrrateado().subtract(deducciones));
        return d;
    }

    /** Builds a Nomina whose totals are the sums of the given lines (as NominaService.calcular does). */
    private Nomina buildNominaConLineas(DetalleNomina... lineas) {
        Nomina nomina = buildNomina();
        List<DetalleNomina> detalles = new ArrayList<>(List.of(lineas));
        nomina.setDetalles(detalles);
        nomina.setTotalBruto(sumar(detalles, DetalleNomina::getBrutoProrrateado));
        nomina.setTotalDeducciones(sumar(detalles, DetalleNomina::getTotalDeducciones));
        nomina.setTotalNeto(sumar(detalles, DetalleNomina::getSalarioNeto));
        nomina.setTotalCcssPatronal(sumar(detalles, DetalleNomina::getCcssPatronal));
        nomina.setTotalAportePatronalSolidarista(sumar(detalles, DetalleNomina::getAportePatronalSolidarista));
        return nomina;
    }

    private static BigDecimal sumar(List<DetalleNomina> detalles,
                                    java.util.function.Function<DetalleNomina, BigDecimal> campo) {
        return detalles.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<DetalleAsiento> lineas(AsientoContable asiento, CuentaContable cuenta, boolean debe) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta().getIdCuenta().equals(cuenta.getIdCuenta()))
                .filter(d -> debe ? d.getDebe().signum() > 0 : d.getHaber().signum() > 0)
                .toList();
    }

    private static BigDecimal debe(AsientoContable asiento, CuentaContable cuenta) {
        return lineas(asiento, cuenta, true).stream().map(DetalleAsiento::getDebe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal haber(AsientoContable asiento, CuentaContable cuenta) {
        return lineas(asiento, cuenta, false).stream().map(DetalleAsiento::getHaber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CuentaContable buildCuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        // subcuentas list defaults to empty ArrayList → puedeRecibirMovimientos() = true
        return c;
    }

    private ParametroContable buildParam(String clave, CuentaContable cuenta) {
        return ParametroContable.builder()
                .clave(clave)
                .cuentaContable(cuenta)
                .build();
    }

    private void seedAllKeys() {
        seedBaseKeys();
        // Stub generarNumeroAsiento() path
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
    }

    /** The seven base NOMINA_* keys, without the entry-number stub (for tests that fail before numbering). */
    private void seedBaseKeys() {
        // Each key gets its own distinct account (different id/codigo to avoid confusion)
        CuentaContable cGastoSueldos       = C_GASTO_SUELDOS;
        CuentaContable cCcssPatronal       = C_CCSS_PATRONAL;
        CuentaContable cCcssObrero         = buildCuenta(12L, "2.1.01.003");
        CuentaContable cIns                = buildCuenta(13L, "2.1.01.004");
        CuentaContable cRenta              = buildCuenta(14L, "2.1.01.005");
        CuentaContable cSalarios           = C_SALARIOS;

        when(parametroContableRepository.findByClave("NOMINA_GASTO_SUELDOS"))
                .thenReturn(Optional.of(buildParam("NOMINA_GASTO_SUELDOS", cGastoSueldos)));
        when(parametroContableRepository.findByClave("NOMINA_GASTO_CARGAS_SOCIALES"))
                .thenReturn(Optional.of(buildParam("NOMINA_GASTO_CARGAS_SOCIALES", C_CARGAS_SOCIALES)));
        when(parametroContableRepository.findByClave("NOMINA_CCSS_PATRONAL_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_CCSS_PATRONAL_POR_PAGAR", cCcssPatronal)));
        when(parametroContableRepository.findByClave("NOMINA_CCSS_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_CCSS_POR_PAGAR", cCcssObrero)));
        when(parametroContableRepository.findByClave("NOMINA_INS_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_INS_POR_PAGAR", cIns)));
        when(parametroContableRepository.findByClave("NOMINA_RENTA_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_RENTA_POR_PAGAR", cRenta)));
        when(parametroContableRepository.findByClave("NOMINA_SALARIOS_POR_PAGAR"))
                .thenReturn(Optional.of(buildParam("NOMINA_SALARIOS_POR_PAGAR", cSalarios)));

    }

    /**
     * Builds a Nomina with known totals such that:
     *   totalBruto = 1_000_000
     *   totalCcssPatronal = 268_300
     *   One DetalleNomina: ccssObrero=108_300, ins=10_000, impuestoRenta=0
     *   totalNeto = totalBruto − ccssObrero − ins − renta = 881_700 (balanced entry)
     */
    private Nomina buildNomina() {
        Nomina nomina = Nomina.builder()
                .id(1L)
                .numero("NOM-2026-0001")
                .periodoInicio(INICIO)
                .periodoFin(FIN)
                .fechaPago(FIN.plusDays(5))
                .tipo(TipoNomina.MENSUAL)
                .estado(EstadoNomina.APROBADA)
                .totalBruto(new BigDecimal("1000000.00"))
                .totalCcssPatronal(new BigDecimal("268300.00"))
                .totalNeto(new BigDecimal("881700.00"))
                .build();

        // One employee detalle: ccssObrero=108300, ins=10000, renta=0
        DetalleNomina detalle = new DetalleNomina();
        detalle.setCcssObrero(new BigDecimal("108300.00"));
        detalle.setIns(new BigDecimal("10000.00"));
        detalle.setImpuestoRenta(BigDecimal.ZERO);
        List<DetalleNomina> detalles = new ArrayList<>();
        detalles.add(detalle);
        nomina.setDetalles(detalles);

        return nomina;
    }
}
