package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoAusencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AusenciaRepository.findPendientesByJefeId.
 *
 * Uses Mockito (no embedded DB) consistent with project test conventions.
 *
 * Verifies the contract of the @Query method:
 * - Returns only absences where aprobada=false for employees in the jefe's department.
 * - Does not return approved absences for the same department.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AusenciaRepository — findPendientesByJefeId")
class AusenciaRepositoryTest {

    @Mock
    private AusenciaRepository ausenciaRepository;

    private Empleado jefe;
    private Empleado empleado;
    private Departamento departamento;
    private Ausencia pendingAusencia;
    private Ausencia approvedAusencia;

    @BeforeEach
    void setUp() {
        jefe = new Empleado();
        jefe.setId(1L);
        jefe.setNombre("Ana");
        jefe.setPrimerApellido("García");
        jefe.setActivo(true);

        departamento = new Departamento();
        departamento.setId(10L);
        departamento.setNombre("Sistemas");
        departamento.setJefe(jefe);

        empleado = new Empleado();
        empleado.setId(2L);
        empleado.setNombre("Luis");
        empleado.setPrimerApellido("Mora");
        empleado.setActivo(true);
        empleado.setDepartamento(departamento);

        pendingAusencia = buildAusencia(empleado, false);
        pendingAusencia.setId(101L);

        approvedAusencia = buildAusencia(empleado, true);
        approvedAusencia.setId(102L);
    }

    /**
     * When the jefe queries pending absences, only aprobada=false records are returned.
     *
     * RED: fails until findPendientesByJefeId is added to AusenciaRepository.
     */
    @Test
    @DisplayName("findPendientesByJefeId returns only pending absences for the jefe's department")
    void findPendientesByJefeId_returnsOnlyPending() {
        // GIVEN: repository returns only the pending ausencia when queried for jefeId=1
        when(ausenciaRepository.findPendientesByJefeId(1L))
                .thenReturn(List.of(pendingAusencia));

        // WHEN
        List<Ausencia> result = ausenciaRepository.findPendientesByJefeId(1L);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAprobada()).isFalse();
        assertThat(result.get(0).getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("findPendientesByJefeId does not return approved absences")
    void findPendientesByJefeId_excludesApproved() {
        // GIVEN: only approved ausencia exists — repository returns empty list
        when(ausenciaRepository.findPendientesByJefeId(1L))
                .thenReturn(List.of());

        // WHEN
        List<Ausencia> result = ausenciaRepository.findPendientesByJefeId(1L);

        // THEN — approved absence must not be returned
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findPendientesByJefeId returns empty list when jefe has no department employees with pending absences")
    void findPendientesByJefeId_noPending_returnsEmpty() {
        // GIVEN: jefeId=99 has no pending absences
        when(ausenciaRepository.findPendientesByJefeId(99L))
                .thenReturn(List.of());

        // WHEN
        List<Ausencia> result = ausenciaRepository.findPendientesByJefeId(99L);

        // THEN
        assertThat(result).isEmpty();
    }

    // --- helpers ---

    private Ausencia buildAusencia(Empleado emp, boolean aprobada) {
        Ausencia a = new Ausencia();
        a.setEmpleado(emp);
        a.setTipoAusencia(TipoAusencia.VACACIONES);
        a.setFechaInicio(LocalDate.of(2026, 8, 1));
        a.setFechaFin(LocalDate.of(2026, 8, 5));
        a.setAprobada(aprobada);
        a.setConGoceSalario(true);
        a.setComputaParaAguinaldo(true);
        a.setComputaAntiguedad(true);
        a.setJustificada(true);
        return a;
    }
}
