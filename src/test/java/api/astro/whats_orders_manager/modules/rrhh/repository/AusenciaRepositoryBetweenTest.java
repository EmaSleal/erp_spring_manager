package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoAusencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Spring Data correctly parses and executes
 * AusenciaRepository.findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween
 * against a real JPA provider (H2 in-memory).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DisplayName("AusenciaRepository — findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween")
class AusenciaRepositoryBetweenTest {

    @Autowired
    private AusenciaRepository ausenciaRepository;

    @Autowired
    private TestEntityManager em;

    private Long empleadoId;

    private static final LocalDate DESDE = LocalDate.of(2026, 7, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 7, 31);

    @BeforeEach
    void setUp() {
        Departamento dept = new Departamento();
        dept.setNombre("RRHH");
        em.persist(dept);

        Puesto puesto = new Puesto();
        puesto.setNombre("Analista");
        puesto.setSalarioBase(new BigDecimal("500000"));
        puesto.setDepartamento(dept);
        em.persist(puesto);

        Empleado empleado = new Empleado();
        empleado.setCedula("1-0001-0001");
        empleado.setNombre("Carlos");
        empleado.setPrimerApellido("Vargas");
        empleado.setDepartamento(dept);
        empleado.setPuesto(puesto);
        empleado.setFechaIngreso(LocalDate.of(2020, 1, 1));
        em.persist(empleado);

        em.flush();
        empleadoId = empleado.getId();
    }

    @Test
    @DisplayName("Returns approved absences whose fechaInicio falls within [desde, hasta]")
    void returnsApprovedAbsencesInWindow() {
        em.persist(buildAusencia(true, LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 12)));
        em.persist(buildAusencia(false, LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 16)));
        em.persist(buildAusencia(true, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)));
        em.flush();

        List<Ausencia> result = ausenciaRepository
                .findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween(empleadoId, DESDE, HASTA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAprobada()).isTrue();
        assertThat(result.get(0).getFechaInicio()).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    @DisplayName("Pending absences within the period are excluded")
    void pendingAbsencesAreExcluded() {
        em.persist(buildAusencia(false, LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 16)));
        em.flush();

        List<Ausencia> result = ausenciaRepository
                .findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween(empleadoId, DESDE, HASTA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Returns empty list when no approved absences fall within the window")
    void returnsEmptyWhenNoApprovedInWindow() {
        em.persist(buildAusencia(true, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)));
        em.flush();

        List<Ausencia> result = ausenciaRepository
                .findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween(empleadoId, DESDE, HASTA);

        assertThat(result).isEmpty();
    }

    private Ausencia buildAusencia(boolean aprobada, LocalDate inicio, LocalDate fin) {
        Ausencia a = new Ausencia();
        a.setEmpleado(em.find(Empleado.class, empleadoId));
        a.setTipoAusencia(TipoAusencia.VACACIONES);
        a.setFechaInicio(inicio);
        a.setFechaFin(fin);
        a.setAprobada(aprobada);
        a.setConGoceSalario(true);
        a.setComputaParaAguinaldo(true);
        a.setComputaAntiguedad(true);
        a.setJustificada(true);
        return a;
    }
}
