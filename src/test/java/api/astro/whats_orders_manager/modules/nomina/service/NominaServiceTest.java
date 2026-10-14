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
import api.astro.whats_orders_manager.modules.rrhh.model.ParametroNominaEmpresa;
import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;
import api.astro.whats_orders_manager.modules.rrhh.repository.AusenciaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.ContratoEmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.ParametroCCSSRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.ParametroNominaEmpresaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.TramoImpuestoSalarioRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.nomina.service.dto.ResultadoCalculoNomina;
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
    private ParametroNominaEmpresaRepository parametroNominaEmpresaRepository;

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
                parametroNominaEmpresaRepository,
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
        when(parametroNominaEmpresaRepository.findVigenteByFecha(FIN))
                .thenReturn(Optional.of(buildParametroEmpresa("0.0533", LocalDate.of(2026, 1, 1), null)));
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
        when(calculosNominaService.calcular(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
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
        when(parametroNominaEmpresaRepository.findVigenteByFecha(FIN))
                .thenReturn(Optional.of(buildParametroEmpresa("0.0533", LocalDate.of(2026, 1, 1), null)));
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
                any(), any(), any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(resultado);

        // WHEN
        service.calcular(2L);

        // THEN: verify calcular was called with horasAusentes = 8 (1 absence × 8h)
        verify(calculosNominaService).calcular(
                eq(new BigDecimal("1000000")),
                eq(new BigDecimal("8")),
                any(), any(), any(), any(), any(), anyInt(), anyBoolean()
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

    // =========================================================================
    // T-010 — ASE employer rate resolved by period date; contabilizar with solidarista + pensión
    // =========================================================================

    @Test
    @DisplayName("T-010 calcular() fails like CCSS when no company payroll parameters are vigente")
    void calcular_sinParametroEmpresaVigente_lanzaIllegalState() {
        Nomina borrador = buildBorradorNomina(7L);
        when(nominaRepository.findById(7L)).thenReturn(Optional.of(borrador));
        when(parametroCCSSRepository.findVigenteByFecha(FIN)).thenReturn(Optional.of(buildCcss()));
        when(parametroNominaEmpresaRepository.findVigenteByFecha(FIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcular(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("parámetros de nómina de la empresa");

        verify(nominaRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-010 vigencia: June payroll uses 0.0533 and July payroll uses 0.0400; aporte persisted and totalled")
    void calcular_tasaAseSegunFechaDelPeriodo() {
        // Real calculation engine to check the persisted contribution
        CalculosNominaService calculosReales = new CalculosNominaService();
        NominaService servicioReal = new NominaService(nominaRepository, detalleNominaRepository,
                empleadoRepository, contratoEmpleadoRepository, ausenciaRepository, parametroCCSSRepository,
                parametroNominaEmpresaRepository, tramoImpuestoSalarioRepository, parametroContableRepository,
                calculosReales, asientoContableService, new NominaMapper(new DetalleNominaMapper()));

        LocalDate finJunio = LocalDate.of(2026, 6, 30);
        Nomina junio = Nomina.builder().id(20L).numero("NOM-2026-0020")
                .periodoInicio(LocalDate.of(2026, 6, 1)).periodoFin(finJunio).fechaPago(finJunio)
                .tipo(TipoNomina.MENSUAL).estado(EstadoNomina.BORRADOR).build();
        junio.setDetalles(new ArrayList<>());
        Nomina julio = buildBorradorNomina(21L);

        when(nominaRepository.findById(20L)).thenReturn(Optional.of(junio));
        when(nominaRepository.findById(21L)).thenReturn(Optional.of(julio));
        when(nominaRepository.save(any(Nomina.class))).thenAnswer(inv -> inv.getArgument(0));
        when(parametroCCSSRepository.findVigenteByFecha(any())).thenReturn(Optional.of(buildCcss()));
        when(parametroNominaEmpresaRepository.findVigenteByFecha(finJunio))
                .thenReturn(Optional.of(buildParametroEmpresa("0.0533", LocalDate.of(2026, 1, 1), finJunio)));
        when(parametroNominaEmpresaRepository.findVigenteByFecha(FIN))
                .thenReturn(Optional.of(buildParametroEmpresa("0.0400", LocalDate.of(2026, 7, 1), null)));
        when(tramoImpuestoSalarioRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2026))
                .thenReturn(buildTramos());

        Empleado afiliado = buildEmpleado(30L, "Laura", "Mena");
        afiliado.setPorcentajeSolidarista(new BigDecimal("0.0300"));
        Empleado noAfiliado = buildEmpleado(31L, "José", "Arias");
        when(empleadoRepository.findByActivoTrue()).thenReturn(List.of(afiliado, noAfiliado));
        ContratoEmpleado contrato = new ContratoEmpleado();
        contrato.setSalarioBruto(new BigDecimal("1000000"));
        when(contratoEmpleadoRepository.findByEmpleadoIdAndActivoTrue(anyLong())).thenReturn(Optional.of(contrato));
        when(ausenciaRepository.findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween(anyLong(), any(), any()))
                .thenReturn(List.of());

        servicioReal.calcular(20L);
        servicioReal.calcular(21L);

        assertThat(junio.getDetalles()).extracting(DetalleNomina::getAportePatronalSolidarista)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("53300.00"), BigDecimal.ZERO);
        assertThat(junio.getTotalAportePatronalSolidarista()).isEqualByComparingTo("53300.00");
        assertThat(julio.getDetalles()).extracting(DetalleNomina::getAportePatronalSolidarista)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("40000.00"), BigDecimal.ZERO);
        assertThat(julio.getTotalAportePatronalSolidarista()).isEqualByComparingTo("40000.00");
        // The ASE contribution does not reduce the net: 1 000 000 − 108 300 − 10 000 − 30 000 = 851 700
        assertThat(julio.getDetalles().get(0).getSalarioNeto()).isEqualByComparingTo("851700.00");
    }

    @Test
    @DisplayName("T-010 contabilizar() of a payroll with solidarista and pensión completes without exception (balanced entry)")
    void contabilizar_conSolidaristaYPension_completaSinExcepcion() {
        AsientoContableRepository asientoRepository = mock(AsientoContableRepository.class);
        AsientoContableService asientoReal = new AsientoContableService(asientoRepository,
                mock(DetalleAsientoRepository.class), mock(CuentaContableRepository.class), parametroContableRepository);
        NominaService servicioReal = new NominaService(nominaRepository, detalleNominaRepository,
                empleadoRepository, contratoEmpleadoRepository, ausenciaRepository, parametroCCSSRepository,
                parametroNominaEmpresaRepository, tramoImpuestoSalarioRepository, parametroContableRepository,
                calculosNominaService, asientoReal, new NominaMapper(new DetalleNominaMapper()));

        // Accounts as seeded by V16 + V27 (ASE liability = solidarista liability)
        mapear("NOMINA_GASTO_SUELDOS", 1L, "5.1.01.001");
        mapear("NOMINA_GASTO_CARGAS_SOCIALES", 10L, "5.1.02.003");
        mapear("NOMINA_SALARIOS_POR_PAGAR", 2L, "2.1.01.001");
        mapear("NOMINA_CCSS_PATRONAL_POR_PAGAR", 3L, "2.1.01.002");
        mapear("NOMINA_CCSS_POR_PAGAR", 4L, "2.1.01.003");
        mapear("NOMINA_INS_POR_PAGAR", 5L, "2.1.01.004");
        mapear("NOMINA_RENTA_POR_PAGAR", 6L, "2.1.01.005");
        mapear("NOMINA_SOLIDARISTA_POR_PAGAR", 7L, "2.1.04.001");
        mapear("NOMINA_PENSION_ALIMENTARIA_POR_PAGAR", 8L, "2.1.04.002");
        mapear("NOMINA_APORTE_PATRONAL_ASE_GASTO", 9L, "5.1.02.001");
        mapear("NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR", 7L, "2.1.04.001");
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        when(asientoRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Payroll computed by the real engine: solidarista 3 % + pensión 50 000, ASE 5.33 %
        ResultadoCalculoNomina r = new CalculosNominaService().calcular(new BigDecimal("1000000"),
                BigDecimal.ZERO, new BigDecimal("0.03"), new BigDecimal("50000"), buildCcss(),
                new BigDecimal("0.0533"), buildTramos(), 0, false);
        DetalleNomina detalle = new DetalleNomina();
        detalle.setBrutoProrrateado(r.brutoProrrateado());
        detalle.setCcssObrero(r.ccssObrero());
        detalle.setIns(r.ins());
        detalle.setImpuestoRenta(r.impuestoRenta());
        detalle.setSolidarista(r.solidarista());
        detalle.setPensionAlimentaria(r.pensionAlimentaria());
        detalle.setTotalDeducciones(r.totalDeducciones());
        detalle.setSalarioNeto(r.salarioNeto());
        detalle.setCcssPatronal(r.ccssPatronal());
        detalle.setAportePatronalSolidarista(r.aportePatronalSolidarista());
        Nomina aprobada = Nomina.builder().id(40L).numero("NOM-2026-0040")
                .periodoInicio(INICIO).periodoFin(FIN).fechaPago(PAGO)
                .tipo(TipoNomina.MENSUAL).estado(EstadoNomina.APROBADA)
                .totalBruto(r.brutoProrrateado()).totalDeducciones(r.totalDeducciones())
                .totalNeto(r.salarioNeto()).totalCcssPatronal(r.ccssPatronal())
                .totalAportePatronalSolidarista(r.aportePatronalSolidarista())
                .build();
        detalle.setNomina(aprobada);
        aprobada.setDetalles(new ArrayList<>(List.of(detalle)));
        when(nominaRepository.findById(40L)).thenReturn(Optional.of(aprobada));
        when(nominaRepository.save(any(Nomina.class))).thenAnswer(inv -> inv.getArgument(0));

        NominaDTO dto = servicioReal.contabilizar(40L);

        assertThat(dto.getEstado()).isEqualTo(EstadoNomina.CONTABILIZADA);
        AsientoContable asiento = captor.getValue();
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        assertThat(asiento.getDetalles()).extracting(DetalleAsiento::getDescripcion)
                .anyMatch(d -> d.startsWith("Ahorro obrero solidarista por pagar"))
                .anyMatch(d -> d.startsWith("Pensiones alimentarias por pagar"))
                .anyMatch(d -> d.startsWith("Aporte patronal asociación solidarista —"));
    }

    // =========================================================================
    // T-014 (ADR-017) — employer CCSS charge goes to NOMINA_GASTO_CARGAS_SOCIALES
    // =========================================================================

    @Test
    @DisplayName("T-014 bruto 1 000 000.00: DR cargas sociales = carga patronal calculada, DR sueldos = bruto, asiento cuadrado")
    void contabilizar_bruto1M_cargaPatronalACargasSociales() {
        AsientoContableRepository asientoRepository = mock(AsientoContableRepository.class);
        AsientoContableService asientoReal = new AsientoContableService(asientoRepository,
                mock(DetalleAsientoRepository.class), mock(CuentaContableRepository.class), parametroContableRepository);
        NominaService servicioReal = new NominaService(nominaRepository, detalleNominaRepository,
                empleadoRepository, contratoEmpleadoRepository, ausenciaRepository, parametroCCSSRepository,
                parametroNominaEmpresaRepository, tramoImpuestoSalarioRepository, parametroContableRepository,
                calculosNominaService, asientoReal, new NominaMapper(new DetalleNominaMapper()));

        // Accounts as left by V29 (ADR-017)
        mapear("NOMINA_GASTO_SUELDOS", 6L, "5.1.02.002");
        mapear("NOMINA_GASTO_CARGAS_SOCIALES", 48L, "5.1.02.003");
        mapear("NOMINA_SALARIOS_POR_PAGAR", 7L, "2.1.04.003");
        mapear("NOMINA_CCSS_PATRONAL_POR_PAGAR", 8L, "2.1.04.004");
        mapear("NOMINA_CCSS_POR_PAGAR", 9L, "2.1.04.005");
        mapear("NOMINA_INS_POR_PAGAR", 10L, "2.1.04.006");
        mapear("NOMINA_RENTA_POR_PAGAR", 11L, "2.1.02.002");
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        when(asientoRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CalculosNominaService calculos = new CalculosNominaService();
        ResultadoCalculoNomina r = calculos.calcular(new BigDecimal("1000000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, buildCcss(),
                BigDecimal.ZERO, buildTramos(), 0, false);
        BigDecimal cargaPatronal = calculos.calcularCargaPatronalCcss(new BigDecimal("1000000.00"), buildCcss());
        DetalleNomina detalle = new DetalleNomina();
        detalle.setBrutoProrrateado(r.brutoProrrateado());
        detalle.setCcssObrero(r.ccssObrero());
        detalle.setIns(r.ins());
        detalle.setImpuestoRenta(r.impuestoRenta());
        detalle.setTotalDeducciones(r.totalDeducciones());
        detalle.setSalarioNeto(r.salarioNeto());
        detalle.setCcssPatronal(r.ccssPatronal());
        Nomina aprobada = Nomina.builder().id(41L).numero("NOM-2026-0041")
                .periodoInicio(INICIO).periodoFin(FIN).fechaPago(PAGO)
                .tipo(TipoNomina.MENSUAL).estado(EstadoNomina.APROBADA)
                .totalBruto(r.brutoProrrateado()).totalDeducciones(r.totalDeducciones())
                .totalNeto(r.salarioNeto()).totalCcssPatronal(r.ccssPatronal())
                .build();
        detalle.setNomina(aprobada);
        aprobada.setDetalles(new ArrayList<>(List.of(detalle)));
        when(nominaRepository.findById(41L)).thenReturn(Optional.of(aprobada));
        when(nominaRepository.save(any(Nomina.class))).thenAnswer(inv -> inv.getArgument(0));

        servicioReal.contabilizar(41L);

        AsientoContable asiento = captor.getValue();
        assertThat(cargaPatronal).isEqualByComparingTo("268300.00");
        assertThat(debePorCuenta(asiento, 48L)).isEqualByComparingTo(cargaPatronal);
        assertThat(debePorCuenta(asiento, 6L)).isEqualByComparingTo("1000000.00");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo("1268300.00");
    }

    @Test
    @DisplayName("T-014 contabilizar() without NOMINA_GASTO_CARGAS_SOCIALES fails in the pre-validation naming the key")
    void contabilizar_sinClaveCargasSociales_fallaEnValidacionPrevia() {
        Nomina aprobada = Nomina.builder().id(42L).numero("NOM-2026-0042")
                .periodoInicio(INICIO).periodoFin(FIN).fechaPago(PAGO)
                .tipo(TipoNomina.MENSUAL).estado(EstadoNomina.APROBADA)
                .build();
        when(nominaRepository.findById(42L)).thenReturn(Optional.of(aprobada));
        mapear("NOMINA_GASTO_SUELDOS", 6L, "5.1.02.002");
        when(parametroContableRepository.findByClave("NOMINA_GASTO_CARGAS_SOCIALES")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.contabilizar(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOMINA_GASTO_CARGAS_SOCIALES");

        verify(asientoContableService, never()).generarAsientoNomina(any());
        verify(nominaRepository, never()).save(any());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static BigDecimal debePorCuenta(AsientoContable asiento, Long idCuenta) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta().getIdCuenta().equals(idCuenta))
                .map(DetalleAsiento::getDebe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void mapear(String clave, Long idCuenta, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(idCuenta);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        when(parametroContableRepository.findByClave(clave))
                .thenReturn(Optional.of(ParametroContable.builder().clave(clave).cuentaContable(c).build()));
    }

    private ParametroNominaEmpresa buildParametroEmpresa(String tasa, LocalDate desde, LocalDate hasta) {
        ParametroNominaEmpresa p = new ParametroNominaEmpresa();
        p.setPorcentajeAportePatronalAse(new BigDecimal(tasa));
        p.setVigenciaDesde(desde);
        p.setVigenciaHasta(hasta);
        return p;
    }
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
                BigDecimal.ZERO,              // aportePatronalSolidarista
                new BigDecimal("106470.00"),  // totalDeducciones
                new BigDecimal("793530.00")   // salarioNeto
        );
    }
}
