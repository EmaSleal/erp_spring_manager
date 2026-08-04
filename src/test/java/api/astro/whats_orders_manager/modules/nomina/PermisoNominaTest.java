package api.astro.whats_orders_manager.modules.nomina;

import api.astro.whats_orders_manager.modules.seguridad.config.MatrizPermisos;
import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD RED phase — NOMINA_* permission wiring tests.
 *
 * Written BEFORE NOMINA_* values are added to Permiso enum and MatrizPermisos.
 * Will fail to compile until the enum values are present; will pass once
 * the production changes (1.5.2 and 1.5.3) are applied.
 *
 * Scenarios:
 * - All 6 NOMINA_* values exist in the Permiso enum
 * - Each has correct category "Nomina"
 * - NOMINA_CONTABILIZAR and NOMINA_ANULAR are critical
 * - GERENTE role contains all 6 in MatrizPermisos
 * - ADMIN role auto-contains all 6 via EnumSet.allOf
 */
@DisplayName("Permiso enum — NOMINA_* values and MatrizPermisos wiring")
class PermisoNominaTest {

    private static final Set<Permiso> EXPECTED_NOMINA_PERMISOS = EnumSet.of(
            Permiso.NOMINA_VER,
            Permiso.NOMINA_CREAR,
            Permiso.NOMINA_CALCULAR,
            Permiso.NOMINA_APROBAR,
            Permiso.NOMINA_CONTABILIZAR,
            Permiso.NOMINA_ANULAR
    );

    // ── Enum value presence ───────────────────────────────────────────────────

    @Test
    @DisplayName("All 6 NOMINA_* values exist in the Permiso enum")
    void allNominaPermisosExist() {
        Set<Permiso> allPermisos = EnumSet.allOf(Permiso.class);
        assertThat(allPermisos).containsAll(EXPECTED_NOMINA_PERMISOS);
    }

    // ── Category ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Each NOMINA_* value has category 'Nomina'")
    void nominaPermisosHaveCorrectCategory() {
        for (Permiso permiso : EXPECTED_NOMINA_PERMISOS) {
            assertThat(permiso.getCategoria())
                    .as("Category for %s should be 'Nomina'", permiso)
                    .isEqualTo("Nomina");
        }
    }

    // ── Critical permissions ──────────────────────────────────────────────────

    @Test
    @DisplayName("NOMINA_CONTABILIZAR is marked as critical")
    void nominaContabilizarIsCritico() {
        assertThat(Permiso.NOMINA_CONTABILIZAR.esCritico()).isTrue();
    }

    @Test
    @DisplayName("NOMINA_ANULAR is marked as critical")
    void nominaAnularIsCritico() {
        assertThat(Permiso.NOMINA_ANULAR.esCritico()).isTrue();
    }

    @Test
    @DisplayName("NOMINA_VER, NOMINA_CREAR, NOMINA_CALCULAR, NOMINA_APROBAR are NOT critical")
    void nominalNonCriticalPermisosAreNotCritico() {
        for (Permiso permiso : EnumSet.of(Permiso.NOMINA_VER, Permiso.NOMINA_CREAR,
                Permiso.NOMINA_CALCULAR, Permiso.NOMINA_APROBAR)) {
            assertThat(permiso.esCritico())
                    .as("%s should not be critical", permiso)
                    .isFalse();
        }
    }

    // ── MatrizPermisos — GERENTE ──────────────────────────────────────────────

    @Test
    @DisplayName("GERENTE role contains all 6 NOMINA_* permissions in MatrizPermisos")
    void gerenteHasAllNominaPermisos() {
        Set<Permiso> gerentePermisos = MatrizPermisos.getPermisos("GERENTE");
        assertThat(gerentePermisos).containsAll(EXPECTED_NOMINA_PERMISOS);
    }

    // ── MatrizPermisos — ADMIN (auto via EnumSet.allOf) ──────────────────────

    @Test
    @DisplayName("ADMIN role auto-contains all 6 NOMINA_* permissions via EnumSet.allOf")
    void adminHasAllNominaPermisos() {
        Set<Permiso> adminPermisos = MatrizPermisos.getPermisos("ADMIN");
        assertThat(adminPermisos).containsAll(EXPECTED_NOMINA_PERMISOS);
    }
}
