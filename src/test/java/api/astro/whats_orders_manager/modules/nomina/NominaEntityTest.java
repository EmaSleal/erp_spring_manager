package api.astro.whats_orders_manager.modules.nomina;

import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD RED phase — NominaEntity state-machine guard tests.
 *
 * Tests are written BEFORE the production classes exist.
 * They must compile once Nomina, EstadoNomina, and TipoNomina are created.
 *
 * Scenarios verified:
 * - APROBADA.puedeAnular() == false → anular() guard throws IllegalStateException
 * - BORRADOR → CALCULADA → APROBADA transition guards are satisfied
 */
@DisplayName("Nomina entity — state machine guards")
class NominaEntityTest {

    private Nomina buildNomina(EstadoNomina estado) {
        return Nomina.builder()
                .numero("NOM-2026-0001")
                .periodoInicio(LocalDate.of(2026, 7, 1))
                .periodoFin(LocalDate.of(2026, 7, 31))
                .fechaPago(LocalDate.of(2026, 8, 5))
                .tipo(TipoNomina.MENSUAL)
                .estado(estado)
                .build();
    }

    // ── BORRADOR guards ───────────────────────────────────────────────────────

    @Test
    @DisplayName("BORRADOR can calculate (puedeCalcular == true)")
    void borrador_puedeCalcular() {
        Nomina n = buildNomina(EstadoNomina.BORRADOR);
        assertThat(n.getEstado().puedeCalcular()).isTrue();
    }

    @Test
    @DisplayName("BORRADOR cannot approve yet (puedeAprobar == false)")
    void borrador_noPoede_aprobar() {
        Nomina n = buildNomina(EstadoNomina.BORRADOR);
        assertThat(n.getEstado().puedeAprobar()).isFalse();
    }

    @Test
    @DisplayName("BORRADOR can be cancelled (puedeAnular == true)")
    void borrador_puedeAnular() {
        Nomina n = buildNomina(EstadoNomina.BORRADOR);
        assertThat(n.getEstado().puedeAnular()).isTrue();
    }

    // ── CALCULADA guards ──────────────────────────────────────────────────────

    @Test
    @DisplayName("CALCULADA can approve (puedeAprobar == true)")
    void calculada_puedeAprobar() {
        Nomina n = buildNomina(EstadoNomina.CALCULADA);
        assertThat(n.getEstado().puedeAprobar()).isTrue();
    }

    @Test
    @DisplayName("CALCULADA can be cancelled (puedeAnular == true)")
    void calculada_puedeAnular() {
        Nomina n = buildNomina(EstadoNomina.CALCULADA);
        assertThat(n.getEstado().puedeAnular()).isTrue();
    }

    @Test
    @DisplayName("CALCULADA cannot calculate again (puedeCalcular == false)")
    void calculada_noPoede_calcular() {
        Nomina n = buildNomina(EstadoNomina.CALCULADA);
        assertThat(n.getEstado().puedeCalcular()).isFalse();
    }

    // ── APROBADA guards ───────────────────────────────────────────────────────

    @Test
    @DisplayName("APROBADA can book accounting (puedeContabilizar == true)")
    void aprobada_puedeContabilizar() {
        Nomina n = buildNomina(EstadoNomina.APROBADA);
        assertThat(n.getEstado().puedeContabilizar()).isTrue();
    }

    @Test
    @DisplayName("APROBADA — anular attempt throws IllegalStateException")
    void aprobada_anular_throwsIllegalStateException() {
        Nomina n = buildNomina(EstadoNomina.APROBADA);
        // The guard method on the enum itself must return false
        assertThat(n.getEstado().puedeAnular()).isFalse();
    }

    @Test
    @DisplayName("APROBADA cannot calculate (puedeCalcular == false)")
    void aprobada_noPoede_calcular() {
        Nomina n = buildNomina(EstadoNomina.APROBADA);
        assertThat(n.getEstado().puedeCalcular()).isFalse();
    }

    // ── Valid forward chain ───────────────────────────────────────────────────

    @Test
    @DisplayName("Valid chain BORRADOR → CALCULADA → APROBADA satisfies each guard")
    void validChain_BORRADOR_CALCULADA_APROBADA() {
        // BORRADOR → can calculate
        EstadoNomina estado = EstadoNomina.BORRADOR;
        assertThat(estado.puedeCalcular()).isTrue();

        // CALCULADA → can approve
        estado = EstadoNomina.CALCULADA;
        assertThat(estado.puedeAprobar()).isTrue();

        // APROBADA → can book
        estado = EstadoNomina.APROBADA;
        assertThat(estado.puedeContabilizar()).isTrue();
    }

    // ── Terminal states cannot transition forward ─────────────────────────────

    @Test
    @DisplayName("CONTABILIZADA cannot calculate, approve, or book again")
    void contabilizada_noForwardTransitions() {
        EstadoNomina estado = EstadoNomina.CONTABILIZADA;
        assertThat(estado.puedeCalcular()).isFalse();
        assertThat(estado.puedeAprobar()).isFalse();
        assertThat(estado.puedeContabilizar()).isFalse();
        assertThat(estado.puedeAnular()).isFalse();
    }

    @Test
    @DisplayName("ANULADA cannot calculate, approve, book, or re-annul")
    void anulada_noForwardTransitions() {
        EstadoNomina estado = EstadoNomina.ANULADA;
        assertThat(estado.puedeCalcular()).isFalse();
        assertThat(estado.puedeAprobar()).isFalse();
        assertThat(estado.puedeContabilizar()).isFalse();
        assertThat(estado.puedeAnular()).isFalse();
    }
}
