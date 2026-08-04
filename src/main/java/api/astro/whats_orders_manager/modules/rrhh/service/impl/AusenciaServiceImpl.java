package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.dto.AusenciaDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.AusenciaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.AusenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AusenciaServiceImpl implements AusenciaService {

    private final AusenciaRepository ausenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public Ausencia registrar(AusenciaDTO dto) {
        Empleado empleado = empleadoRepository.findById(dto.getEmpleadoId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Empleado no encontrado: " + dto.getEmpleadoId()));

        // Validate no overlap with approved ausencias
        List<Ausencia> existing = ausenciaRepository.findByEmpleadoId(dto.getEmpleadoId());
        for (Ausencia existente : existing) {
            if (Boolean.TRUE.equals(existente.getAprobada())) {
                if (overlaps(dto.getFechaInicio(), dto.getFechaFin(),
                        existente.getFechaInicio(), existente.getFechaFin())) {
                    throw new IllegalArgumentException(
                            "El rango de ausencia se traslapa con una ausencia aprobada existente "
                                    + "(" + existente.getFechaInicio() + " — " + existente.getFechaFin() + ")");
                }
            }
        }

        Ausencia ausencia = new Ausencia();
        ausencia.setEmpleado(empleado);
        ausencia.setTipoAusencia(dto.getTipoAusencia());
        ausencia.setFechaInicio(dto.getFechaInicio());
        ausencia.setFechaFin(dto.getFechaFin());
        ausencia.setDescripcion(dto.getDescripcion());
        ausencia.setConGoceSalario(dto.getConGoceSalario() != null ? dto.getConGoceSalario() : true);
        ausencia.setComputaParaAguinaldo(dto.getComputaParaAguinaldo() != null ? dto.getComputaParaAguinaldo() : true);
        ausencia.setComputaAntiguedad(dto.getComputaAntiguedad() != null ? dto.getComputaAntiguedad() : true);
        ausencia.setJustificada(dto.getJustificada() != null ? dto.getJustificada() : true);
        ausencia.setAprobada(false);
        ausencia.setEntidadCertificante(dto.getEntidadCertificante());
        ausencia.setNumeroBoleta(dto.getNumeroBoleta());
        ausencia.setPorcentajePatrono(dto.getPorcentajePatrono());
        ausencia.setPorcentajeSubsidio(dto.getPorcentajeSubsidio());

        // Resolve optional aprobadaPor
        if (dto.getAprobadaPorId() != null) {
            Usuario aprobador = usuarioRepository.findById(dto.getAprobadaPorId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Usuario no encontrado: " + dto.getAprobadaPorId()));
            ausencia.setAprobadaPor(aprobador);
        }

        Ausencia saved = ausenciaRepository.save(ausencia);
        log.info("Ausencia registrada: id={} tipo={} empleado={}",
                saved.getId(), saved.getTipoAusencia(), empleado.getId());
        return saved;
    }

    @Override
    public void aprobar(Long id, Integer usuarioAprobadorId) {
        Ausencia ausencia = ausenciaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ausencia no encontrada: " + id));

        Usuario aprobador = usuarioRepository.findById(usuarioAprobadorId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Usuario aprobador no encontrado: " + usuarioAprobadorId));

        ausencia.setAprobada(true);
        ausencia.setAprobadaPor(aprobador);
        ausencia.setFechaAprobacion(LocalDateTime.now());

        ausenciaRepository.save(ausencia);
        log.info("Ausencia aprobada: id={} por usuario={}", id, usuarioAprobadorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ausencia> findByEmpleado(Long empleadoId) {
        return ausenciaRepository.findByEmpleadoId(empleadoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ausencia> findAll() {
        return ausenciaRepository.findAll();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns true if [start1, end1] overlaps [start2, end2] (inclusive).
     */
    private boolean overlaps(LocalDate start1, LocalDate end1,
                             LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }
}
