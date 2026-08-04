package api.astro.whats_orders_manager.modules.nomina.service;

import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.nomina.dto.NominaDTO;
import api.astro.whats_orders_manager.modules.nomina.dto.NominaResumenDTO;
import api.astro.whats_orders_manager.modules.nomina.dto.mapper.NominaMapper;
import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.model.DetalleNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import api.astro.whats_orders_manager.modules.nomina.repository.DetalleNominaRepository;
import api.astro.whats_orders_manager.modules.nomina.repository.NominaRepository;
import api.astro.whats_orders_manager.modules.nomina.service.dto.ResultadoCalculoNomina;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestration service for the Nómina (payroll) module.
 *
 * Responsibilities:
 * - Lifecycle: crear → calcular → aprobar → contabilizar / anular
 * - Cross-module coordination: employees, contracts, absences, CCSS params, tax brackets
 * - Delegates gross-to-net math to {@link CalculosNominaService}
 *
 * State machine enforcement is delegated to {@link EstadoNomina} helpers.
 * Violations throw {@link IllegalStateException} with the current estado message.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NominaService {

    private static final String[] NOMINA_KEYS = {
            "NOMINA_GASTO_SUELDOS",
            "NOMINA_CCSS_PATRONAL_POR_PAGAR",
            "NOMINA_CCSS_POR_PAGAR",
            "NOMINA_INS_POR_PAGAR",
            "NOMINA_RENTA_POR_PAGAR",
            "NOMINA_SALARIOS_POR_PAGAR"
    };

    private final NominaRepository nominaRepository;
    private final DetalleNominaRepository detalleNominaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ContratoEmpleadoRepository contratoEmpleadoRepository;
    private final AusenciaRepository ausenciaRepository;
    private final ParametroCCSSRepository parametroCCSSRepository;
    private final TramoImpuestoSalarioRepository tramoImpuestoSalarioRepository;
    private final ParametroContableRepository parametroContableRepository;
    private final CalculosNominaService calculosNominaService;
    private final AsientoContableService asientoContableService;
    private final NominaMapper nominaMapper;

    // ── crear ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new payroll run in BORRADOR state.
     *
     * @throws IllegalArgumentException if a nomina for the same period and tipo already exists
     */
    @Transactional
    public NominaDTO crear(LocalDate periodoInicio, LocalDate periodoFin,
                           LocalDate fechaPago, TipoNomina tipo) {

        if (nominaRepository.existsByPeriodoInicioAndPeriodoFinAndTipo(periodoInicio, periodoFin, tipo)) {
            throw new IllegalArgumentException(
                    "Ya existe una nómina para este período y tipo. " +
                    "período: " + periodoInicio + " – " + periodoFin + ", tipo: " + tipo);
        }

        long count = nominaRepository.count();
        String numero = "NOM-" + periodoInicio.getYear() + "-" + String.format("%04d", count + 1);

        Nomina nomina = Nomina.builder()
                .numero(numero)
                .periodoInicio(periodoInicio)
                .periodoFin(periodoFin)
                .fechaPago(fechaPago)
                .tipo(tipo)
                .estado(EstadoNomina.BORRADOR)
                .totalBruto(BigDecimal.ZERO)
                .totalDeducciones(BigDecimal.ZERO)
                .totalNeto(BigDecimal.ZERO)
                .totalCcssPatronal(BigDecimal.ZERO)
                .detalles(new ArrayList<>())
                .build();

        Nomina saved = nominaRepository.save(nomina);
        log.info("Nómina creada: {} ({})", saved.getNumero(), saved.getTipo());
        return nominaMapper.toDTO(saved);
    }

    // ── calcular ──────────────────────────────────────────────────────────────

    /**
     * Executes gross-to-net calculation for all active employees.
     *
     * <p>Requires BORRADOR estado. Clears any previously computed detalles
     * to support re-runs within the same state.
     *
     * @throws IllegalStateException if estado is not BORRADOR
     * @throws IllegalStateException if no CCSS parameters exist for periodoFin
     */
    @Transactional
    public NominaDTO calcular(Long nominaId) {
        Nomina nomina = findOrThrow(nominaId);

        if (!nomina.puedeCalcular()) {
            throw new IllegalStateException(
                    "Estado actual no permite calcular: " + nomina.getEstado());
        }

        // Load regulatory parameters
        ParametroCCSS ccss = parametroCCSSRepository.findVigenteByFecha(nomina.getPeriodoFin())
                .orElseThrow(() -> new IllegalStateException(
                        "No hay parámetros CCSS vigentes para la fecha: " + nomina.getPeriodoFin()));

        List<TramoImpuestoSalario> tramos = tramoImpuestoSalarioRepository
                .findByAnioVigenciaOrderByLimiteInferiorAsc(nomina.getPeriodoFin().getYear());

        // Clear existing detalles (recalculation support)
        nomina.getDetalles().clear();

        // Iterate active employees
        List<Empleado> empleados = empleadoRepository.findByActivoTrue();

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDeducciones = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalCcssPatronal = BigDecimal.ZERO;

        for (Empleado empleado : empleados) {
            Optional<ContratoEmpleado> contratoOpt =
                    contratoEmpleadoRepository.findByEmpleadoIdAndActivoTrue(empleado.getId());

            if (contratoOpt.isEmpty()) {
                log.warn("Empleado {} sin contrato activo — omitido de nómina {}",
                        empleado.getId(), nominaId);
                continue;
            }

            ContratoEmpleado contrato = contratoOpt.get();

            // Load approved unpaid absences in period
            List<Ausencia> ausencias = ausenciaRepository
                    .findByEmpleadoIdAndAprobadaTrueAndFechaInicioBetween(
                            empleado.getId(),
                            nomina.getPeriodoInicio(),
                            nomina.getPeriodoFin());

            // horasAusentes = count of unpaid absences × 8h (one absence = one full day = 8h)
            BigDecimal horasAusentes = new BigDecimal(
                    ausencias.stream()
                            .filter(a -> Boolean.FALSE.equals(a.getConGoceSalario()))
                            .count() * 8L);

            // Calculate gross-to-net
            ResultadoCalculoNomina resultado = calculosNominaService.calcular(
                    contrato.getSalarioBruto(),
                    horasAusentes,
                    empleado.getPorcentajeSolidarista(),
                    empleado.getMontoPensionAlimentaria(),
                    ccss,
                    tramos,
                    empleado.getHijosCargaFamiliar() != null ? empleado.getHijosCargaFamiliar() : 0,
                    Boolean.TRUE.equals(empleado.getConyugeCargaFamiliar())
            );

            // Build DetalleNomina
            DetalleNomina detalle = new DetalleNomina();
            detalle.setNomina(nomina);
            detalle.setEmpleado(empleado);
            detalle.setSalarioBase(contrato.getSalarioBruto());
            detalle.setHorasAusentes(horasAusentes);
            detalle.setBrutoProrrateado(resultado.brutoProrrateado());
            detalle.setCcssObrero(resultado.ccssObrero());
            detalle.setIns(resultado.ins());
            detalle.setImpuestoRenta(resultado.impuestoRenta());
            detalle.setCreditoFamiliar(resultado.creditoFamiliar());
            detalle.setSolidarista(resultado.solidarista());
            detalle.setPensionAlimentaria(resultado.pensionAlimentaria());
            detalle.setTotalDeducciones(resultado.totalDeducciones());
            detalle.setSalarioNeto(resultado.salarioNeto());
            detalle.setCcssPatronal(resultado.ccssPatronal());

            nomina.getDetalles().add(detalle);

            // Accumulate totals
            totalBruto = totalBruto.add(resultado.brutoProrrateado());
            totalDeducciones = totalDeducciones.add(resultado.totalDeducciones());
            totalNeto = totalNeto.add(resultado.salarioNeto());
            totalCcssPatronal = totalCcssPatronal.add(resultado.ccssPatronal());
        }

        nomina.setTotalBruto(totalBruto);
        nomina.setTotalDeducciones(totalDeducciones);
        nomina.setTotalNeto(totalNeto);
        nomina.setTotalCcssPatronal(totalCcssPatronal);
        nomina.setEstado(EstadoNomina.CALCULADA);

        Nomina saved = nominaRepository.save(nomina);
        log.info("Nómina {} calculada: {} empleados, totalNeto={}",
                saved.getNumero(), saved.getDetalles().size(), saved.getTotalNeto());
        return nominaMapper.toDTO(saved);
    }

    // ── aprobar ───────────────────────────────────────────────────────────────

    /**
     * Transitions nomina from CALCULADA to APROBADA.
     *
     * @throws IllegalStateException if estado is not CALCULADA
     */
    @Transactional
    public NominaDTO aprobar(Long nominaId) {
        Nomina nomina = findOrThrow(nominaId);

        if (!nomina.puedeAprobar()) {
            throw new IllegalStateException(
                    "Estado actual no permite aprobar: " + nomina.getEstado());
        }

        nomina.setEstado(EstadoNomina.APROBADA);
        Nomina saved = nominaRepository.save(nomina);
        log.info("Nómina {} aprobada", saved.getNumero());
        return nominaMapper.toDTO(saved);
    }

    // ── contabilizar ──────────────────────────────────────────────────────────

    /**
     * Validates parametros_contables and transitions nomina from APROBADA to CONTABILIZADA.
     *
     * <p>In PR3 the accounting entry generation (AsientoContableService.generarAsientoNomina)
     * is intentionally left out — it will be added in PR4 with a single line insertion here.
     *
     * @throws IllegalStateException if estado is not APROBADA
     * @throws IllegalStateException if any required NOMINA_* parametro_contable key is missing
     */
    @Transactional
    public NominaDTO contabilizar(Long nominaId) {
        Nomina nomina = findOrThrow(nominaId);

        if (!nomina.puedeContabilizar()) {
            throw new IllegalStateException(
                    "Estado actual no permite contabilizar: " + nomina.getEstado());
        }

        // Validate all 6 NOMINA_* accounting parameters exist before any side-effect
        for (String clave : NOMINA_KEYS) {
            parametroContableRepository.findByClave(clave)
                    .orElseThrow(() -> new IllegalStateException(
                            "Parámetro contable no configurado: " + clave));
        }

        asientoContableService.generarAsientoNomina(nomina);

        nomina.setEstado(EstadoNomina.CONTABILIZADA);
        Nomina saved = nominaRepository.save(nomina);
        log.info("Nómina {} contabilizada", saved.getNumero());
        return nominaMapper.toDTO(saved);
    }

    // ── anular ────────────────────────────────────────────────────────────────

    /**
     * Transitions nomina from BORRADOR or CALCULADA to ANULADA.
     *
     * @throws IllegalStateException if estado is APROBADA, CONTABILIZADA, or already ANULADA
     */
    @Transactional
    public NominaDTO anular(Long nominaId, String motivo) {
        Nomina nomina = findOrThrow(nominaId);

        if (!nomina.puedeAnular()) {
            throw new IllegalStateException(
                    "Estado actual no permite anular: " + nomina.getEstado());
        }

        nomina.setEstado(EstadoNomina.ANULADA);
        nomina.setMotivoAnulacion(motivo);
        Nomina saved = nominaRepository.save(nomina);
        log.info("Nómina {} anulada. Motivo: {}", saved.getNumero(), motivo);
        return nominaMapper.toDTO(saved);
    }

    // ── listar / obtener ──────────────────────────────────────────────────────

    /** Returns all payrolls as summary projections. */
    public List<NominaResumenDTO> listar() {
        return nominaMapper.toResumenDTOList(nominaRepository.findAll());
    }

    /** Returns a full nomina with detail lines. */
    public NominaDTO obtener(Long nominaId) {
        return nominaMapper.toDTO(findOrThrow(nominaId));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Nomina findOrThrow(Long id) {
        return nominaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nómina no encontrada: " + id));
    }
}
