package api.astro.whats_orders_manager.modules.nomina.service;

import api.astro.whats_orders_manager.modules.nomina.dto.NominaDTO;
import api.astro.whats_orders_manager.modules.nomina.dto.mapper.DetalleNominaMapper;
import api.astro.whats_orders_manager.modules.nomina.dto.mapper.NominaMapper;
import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.model.DetalleNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import api.astro.whats_orders_manager.modules.nomina.repository.DetalleNominaRepository;
import api.astro.whats_orders_manager.modules.nomina.repository.NominaRepository;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import api.astro.whats_orders_manager.modules.rrhh.model.ContratoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.model.ParametroCCSS;
import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;
import api.astro.whats_orders_manager.modules.rrhh.repository.AusenciaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.ContratoEmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.ParametroCCSSRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.TramoImpuestoSalarioRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.nomina.service.dto.ResultadoCalculoNomina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD RED phase — NominaService unit tests.
 *
 * Written BEFORE NominaService implementation exists.
 * Uses Mockito (no Spring context), consistent with existing service test patterns.
 *
 * Scenarios verified:
 * 3.2.1 — crear() rejects duplicate period + tipo
 * 3.2.2 — crear() allows different tipo in same period
 * 3.2.3 — calcular() rejects non-BORRADOR estado
 * 3.2.4 — calcular() skips employee without active contract
 * 3.2.5 — calcular() absent hours reduce brutoProrrateado
 * 3.2.6 — anular() rejects APROBADA nomina
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NominaService — unit tests")
class NominaServiceTest {

    @Mock
    private NominaRepository nominaRepository;

    @Mock
    private DetalleNominaRepository detalleNominaRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private ContratoEmpleadoRepository contratoEmpleadoRepository;

    @Mock
    private AusenciaRepository ausenciaRepository;

    @Mock
    private ParametroCCSSRepository parametroCCSSRepository;

    @Mock
    private TramoImpuestoSalarioRepository tramoImpuestoSalarioRepository;

    @Mock
    private ParametroContableRepository parametroContableRepository;

    @Mock
    private CalculosNominaService calculosNominaService;

    @Mock
    private AsientoContableService asientoContableService;

    private NominaService service;

    // ── Common fixtures ──────────────────────────────────────────────────────

    private static final LocalDate INICIO = LocalDate.of(2026, 7, 1);
    private static final LocalDate FIN    = LocalDate.of(2026, 7, 31);
    private static final LocalDate PAGO   = LocalDate.of(2026, 8, 5);

    @BeforeEach
    void setUp() {
        NominaMapper nominaMapper = new NominaMapper(new DetalleNominaMapper());
        service = new NominaService(
                nominaRepository,
                detalleNominaRepository,
                empleadoRepository,
                contratoEmpleadoRepository,
                ausenciaRepository,
                parametroCCSSRepository,
                tramoImpuestoSalarioRepository,
                parametroContableRepository,
                calculosNominaService,
                asientoContableService,
                nominaMapper
        );
    }

    // =========================================================================
    // 3.2.1 — crear() rejects duplicate period + tipo
    // =========================================================================

    @Test
    @DisplayName("3.2.1 crear() throws IllegalArgumentException when same period and tipo already exist")
    void crear_duplicatePeriodAndTipo_throwsIllegalArgument() {
        // GIVEN: a MENSUAL nomina for 2026-07 already exists
        when(nominaRepository.existsByPeriodoInicioAndPeriodoFinAndTipo(INICIO, FIN, TipoNomina.MENSUAL))
                .thenReturn(true);

        // WHEN / THEN
        assertThatThrownBy(() -> service.crear(INICIO, FIN, PAGO, TipoNomina.MENSUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("período");

        verify(nominaRepository, never()).save(any());
    }

    // =========================================================================
    // 3.2.2 — crear() allows different tipo in same period
    // =========================================================================

    @Test
    @DisplayName("3.2.2 crear() succeeds when same period but different tipo (QUINCENAL vs MENSUAL)")
    void crear_differentTipoSamePeriod_succeeds() {
        // GIVEN: MENSUAL exists, but we create QUINCENAL
        when(nominaRepository.existsByPeriodoInicioAndPeriodoFinAndTipo(INICIO, FIN, TipoNomina.QUINCENAL))
                .thenReturn(false);

        Nomina saved = Nomina.builder()
                .id(2L)
                .numero("NOM-2026-0002")
                .periodoInicio(INICIO)
                .periodoFin(FIN)
                .fechaPago(PAGO)
                .tipo(TipoNomina.QUINCENAL)
                .estado(EstadoNomina.BORRADOR)
                .build();
        when(nominaRepository.save(any(Nomina.class))).thenReturn(saved);
        when(nominaRepository.count()).thenReturn(1L);

        // WHEN
        NominaDTO result = service.crear(INICIO, FIN, PAGO, TipoNomina.QUINCENAL);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getEstado()).isEqualTo(EstadoNomina.BORRADOR);
        verify(nominaRepository).save(any(Nomina.class));
    }

    // =========================================================================
    // 3.2.3 — calcular() rejects non-BORRADOR estado
    // =========================================================================

    @Test
    @DisplayName("3.2.3 calcular() throws IllegalStateException when nomina is in APROBADA estado")
    void calcular_nonBorrador_throwsIllegalState() {
        // GIVEN: nomina in APROBADA
        Nomina aprobada = Nomina.builder()
                .id(1L)
                .numero("NOM-2026-0001")
                .periodoInicio(INICIO)
                .periodoFin(FIN)
                .fechaPago(PAGO)
                .tipo(TipoNomina.MENSUAL)
                .estado(EstadoNomina.APROBADA)
                .build();
        when(nominaRepository.findById(1L)).thenReturn(Optional.of(aprobada));

        // WHEN / THEN
        assertThatThrownBy(() -> service.calcular(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APROBADA");
    }

    // =========================================================================
    // 3.2.4 — calcular() skips employee without active contract
    // =========================================================================

    @Test
    @DisplayName("3.2.4 calcular() skips employee with no active contract and proceeds for others")
    void calcular_employeeWithoutContract_isSkipped() {
        // GIVEN: nomina in BORRADOR
        Nomina borrador = buildBorradorNomina(1L);
        when(nominaRepository.findById(1L)).thenReturn(Optional.of(borrador));
        when(nominaRepository.save(any(Nomina.class))).thenReturn(borrador);

        // CCSS params
        ParametroCCSS ccss = buildCcss();
        when(parametroCCSSRepository.findVigenteByFecha(FIN)).thenReturn(Optional.of(ccss));
        when(tramoImpuestoSalarioRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2026))
                .thenReturn(buildTramos());

        // Two employees: one with contract, one without
        Empleado empConContrato = buildEmpleado(10L, "Ana", "Ríos");
        Empleado empSinContrato = buildEmpleado(20L, "Pedro", "Mora");

        when(empleadoRepository.findByActivoTrue())
                .thenReturn(List.of(empConContrato, empSinContrato));

        ContratoEmpleado contrato = new ContratoEmpleado();
        contrato.setSalarioBruto(new BigDecimal("900000"));
        when(contratoEmpleadoRepository.findByEmpleadoIdAndActivoTrue(10L))
                .thenReturn(Optional.of(contrato));
        when(contratoEmpleadoRepository.findByEmpleadoIdAndActivoTrue(20L))
                .thenReturn(Optional.empty()); // no contract

        when(ausenciaRepository.findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween(
                eq(10L), any(), any())).thenReturn(List.of());

        ResultadoCalculoNomina resultado = buildResultado();
        when(calculosNominaService.calcular(any(), any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(resultado);

        // WHEN
        NominaDTO dto = service.calcular(1L);

        // THEN: only one detalle (emp 10); emp 20 was skipped
        assertThat(dto.getDetalles()).hasSize(1);
        assertThat(dto.getDetalles().get(0).getEmpleadoId()).isEqualTo(10L);
        assertThat(dto.getEstado()).isEqualTo(EstadoNomina.CALCULADA);
    }

    // =========================================================================
    // 3.2.5 — calcular() absent hours reduce brutoProrrateado
    // =========================================================================

    @Test
    @DisplayName("3.2.5 calcular() passes absent hours so CalculosNominaService reduces bruto")
    void calcular_absentHoursReduceBruto() {
        // GIVEN: nomina in BORRADOR
        Nomina borrador = buildBorradorNomina(2L);
        when(nominaRepository.findById(2L)).thenReturn(Optional.of(borrador));
        when(nominaRepository.save(any(Nomina.class))).thenReturn(borrador);

        ParametroCCSS ccss = buildCcss();
        when(parametroCCSSRepository.findVigenteByFecha(FIN)).thenReturn(Optional.of(ccss));
        when(tramoImpuestoSalarioRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2026))
                .thenReturn(buildTramos());

        Empleado emp = buildEmpleado(5L, "María", "Soto");
        when(empleadoRepository.findByActivoTrue()).thenReturn(List.of(emp));

        ContratoEmpleado contrato = new ContratoEmpleado();
        contrato.setSalarioBruto(new BigDecimal("1000000"));
        when(contratoEmpleadoRepository.findByEmpleadoIdAndActivoTrue(5L))
                .thenReturn(Optional.of(contrato));

        // One approved unpaid absence (conGoceSalario=false → 8 hours deducted)
        Ausencia ausencia = new Ausencia();
        ausencia.setConGoceSalario(false);
        when(ausenciaRepository.findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween(
                eq(5L), any(), any())).thenReturn(List.of(ausencia));

        // Capture what horasAusentes value is passed to CalculosNominaService
        ResultadoCalculoNomina resultado = buildResultado();
        when(calculosNominaService.calcular(
                any(), any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(resultado);

        // WHEN
        service.calcular(2L);

        // THEN: verify calcular was called with horasAusentes = 8 (1 absence × 8h)
        verify(calculosNominaService).calcular(
                eq(new BigDecimal("1000000")),
                eq(new BigDecimal("8")),
                any(), any(), any(), any(), anyInt(), anyBoolean()
        );
    }

    // =========================================================================
    // 3.2.6 — anular() rejects APROBADA nomina
    // =========================================================================

    @Test
    @DisplayName("3.2.6 anular() throws IllegalStateException when nomina is in APROBADA estado")
    void anular_aprobadaNomina_throwsIllegalState() {
        // GIVEN
        Nomina aprobada = Nomina.builder()
                .id(3L)
                .numero("NOM-2026-0003")
                .periodoInicio(INICIO)
                .periodoFin(FIN)
                .fechaPago(PAGO)
                .tipo(TipoNomina.MENSUAL)
                .estado(EstadoNomina.APROBADA)
                .build();
        when(nominaRepository.findById(3L)).thenReturn(Optional.of(aprobada));

        // WHEN / THEN
        assertThatThrownBy(() -> service.anular(3L, "test motivo"))
                .isInstanceOf(IllegalStateException.class);

        verify(nominaRepository, never()).save(any());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Nomina buildBorradorNomina(Long id) {
        Nomina n = Nomina.builder()
                .id(id)
                .numero("NOM-2026-000" + id)
                .periodoInicio(INICIO)
                .periodoFin(FIN)
                .fechaPago(PAGO)
                .tipo(TipoNomina.MENSUAL)
                .estado(EstadoNomina.BORRADOR)
                .build();
        // Initialise mutable detalles list (Builder.Default is not set when using builder())
        n.setDetalles(new ArrayList<>());
        return n;
    }

    private Empleado buildEmpleado(Long id, String nombre, String apellido) {
        Empleado e = new Empleado();
        e.setId(id);
        e.setNombre(nombre);
        e.setPrimerApellido(apellido);
        e.setActivo(true);
        e.setHijosCargaFamiliar(0);
        e.setConyugeCargaFamiliar(false);
        return e;
    }

    private ParametroCCSS buildCcss() {
        ParametroCCSS c = new ParametroCCSS();
        c.setPorcentajeObrero(new BigDecimal("0.1083"));
        c.setPorcentajePatronal(new BigDecimal("0.2683"));
        return c;
    }

    private List<TramoImpuestoSalario> buildTramos() {
        TramoImpuestoSalario exento = new TramoImpuestoSalario();
        exento.setLimiteInferior(BigDecimal.ZERO);
        exento.setLimiteSuperior(new BigDecimal("918000"));
        exento.setPorcentaje(BigDecimal.ZERO);
        exento.setCreditoPorHijo(new BigDecimal("1710"));
        exento.setCreditoPorConyuge(new BigDecimal("2590"));
        exento.setAnioVigencia(2026);
        return List.of(exento);
    }

    private ResultadoCalculoNomina buildResultado() {
        return new ResultadoCalculoNomina(
                new BigDecimal("900000.00"),  // brutoProrrateado
                new BigDecimal("97470.00"),   // ccssObrero
                new BigDecimal("9000.00"),    // ins
                new BigDecimal("793530.00"),  // baseGravable
                BigDecimal.ZERO,              // impuestoRenta
                BigDecimal.ZERO,              // creditoFamiliar
                BigDecimal.ZERO,              // solidarista
                BigDecimal.ZERO,              // pensionAlimentaria
                new BigDecimal("241470.00"),  // ccssPatronal
                new BigDecimal("106470.00"),  // totalDeducciones
                new BigDecimal("793530.00")   // salarioNeto
        );
    }
}
