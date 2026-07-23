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
        // GIVEN: first 5 keys are present, NOMINA_SALARIOS_POR_PAGAR is absent
        CuentaContable cuenta = buildCuenta(1L, "1.1.01");

        when(parametroContableRepository.findByClave("NOMINA_GASTO_SUELDOS"))
                .thenReturn(Optional.of(buildParam("NOMINA_GASTO_SUELDOS", cuenta)));
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
        // totalBruto=1_000_000, totalCcssPatronal=268_300
        // DR sum = 1_000_000 + 268_300 = 1_268_300
        // CR sum = ccssObrero(108_300) + ins(10_000) + renta(0) + neto(882_000)
        //        = 108_300 + 10_000 + 0 + 882_000 = 1_000_300   <- unbalanced intentionally?
        // Actually the balance needs totalBruto + totalCcssPatronal = ccssObrero + ins + renta + totalNeto
        // 1_000_000 + 268_300 = 108_300 + 10_000 + 0 + 1_150_000 = 1_268_300 ✓
        nomina.setTotalNeto(new BigDecimal("1150000.00")); // adjusted to make it balance
        // ccssObrero per detalles = 108_300, ins = 10_000, renta = 0
        // CR total = 108_300 + 10_000 + 0 + 1_150_000 = 1_268_300 = DR total ✓

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
    }

    // =========================================================================
    // 4.2.3 — Persisted asiento links back to nomina
    // =========================================================================

    @Test
    @DisplayName("4.2.3 generarAsientoNomina sets asiento.nomina to the originating Nomina")
    void generarAsientoNomina_linksAsientoToNomina() {
        seedAllKeys();

        Nomina nomina = buildNomina();
        nomina.setTotalNeto(new BigDecimal("1150000.00"));

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
        nomina.setTotalNeto(new BigDecimal("1150000.00"));

        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        AsientoContable result = service.generarAsientoNomina(nomina);

        // THEN
        assertThat(result.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(result.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_NOMINA);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

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
        // Each key gets its own distinct account (different id/codigo to avoid confusion)
        CuentaContable cGastoSueldos       = buildCuenta(10L, "5.1.01");
        CuentaContable cCcssPatronal       = buildCuenta(11L, "5.1.02");
        CuentaContable cCcssObrero         = buildCuenta(12L, "2.1.01");
        CuentaContable cIns                = buildCuenta(13L, "2.1.02");
        CuentaContable cRenta              = buildCuenta(14L, "2.1.03");
        CuentaContable cSalarios           = buildCuenta(15L, "2.1.04");

        when(parametroContableRepository.findByClave("NOMINA_GASTO_SUELDOS"))
                .thenReturn(Optional.of(buildParam("NOMINA_GASTO_SUELDOS", cGastoSueldos)));
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

        // Stub generarNumeroAsiento() path
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
    }

    /**
     * Builds a Nomina with known totals such that:
     *   totalBruto = 1_000_000
     *   totalCcssPatronal = 268_300
     *   One DetalleNomina: ccssObrero=108_300, ins=10_000, impuestoRenta=0
     *   totalNeto is left for caller to set (must equal totalBruto+totalCcssPatronal - ccssObrero - ins - renta)
     *   Default totalNeto = 882_000 (unbalanced; callers override to 1_150_000 for the balance scenarios)
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
                .totalNeto(new BigDecimal("882000.00"))
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
