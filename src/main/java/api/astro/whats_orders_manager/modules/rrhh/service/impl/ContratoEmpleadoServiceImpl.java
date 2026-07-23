package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.dto.ContratoEmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.CausaTerminacion;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoContrato;
import api.astro.whats_orders_manager.modules.rrhh.model.ContratoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.ContratoEmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.ContratoEmpleadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContratoEmpleadoServiceImpl implements ContratoEmpleadoService {

    private final ContratoEmpleadoRepository contratoRepository;
    private final EmpleadoRepository empleadoRepository;

    @Override
    public ContratoEmpleado crear(ContratoEmpleadoDTO dto) {
        Empleado empleado = empleadoRepository.findById(dto.getEmpleadoId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Empleado no encontrado: " + dto.getEmpleadoId()));

        // Guard: temporal contracts require a written justification (CR labor law)
        if ((dto.getTipoContrato() == TipoContrato.PLAZO_FIJO || dto.getTipoContrato() == TipoContrato.OBRA_DETERMINADA)
                && (dto.getJustificacionTemporalidad() == null || dto.getJustificacionTemporalidad().isBlank())) {
            throw new IllegalArgumentException(
                    "justificacionTemporalidad es obligatoria para contratos temporales");
        }

        // Deactivate the existing active contract (if any) — single transaction
        contratoRepository.findByEmpleadoIdAndActivoTrue(dto.getEmpleadoId())
                .ifPresent(existing -> {
                    existing.setActivo(false);
                    contratoRepository.save(existing);
                    log.info("Contrato anterior desactivado: id={}", existing.getId());
                });

        // Build and persist new contract
        ContratoEmpleado contrato = new ContratoEmpleado();
        contrato.setEmpleado(empleado);
        contrato.setTipoContrato(dto.getTipoContrato());
        contrato.setFechaInicio(dto.getFechaInicio());
        contrato.setFechaFin(dto.getFechaFin());
        contrato.setSalarioBruto(dto.getSalarioBruto());
        contrato.setJornada(dto.getJornada());
        contrato.setCargoContratado(dto.getCargoContratado());
        contrato.setJustificacionTemporalidad(dto.getJustificacionTemporalidad());
        contrato.setActivo(true);
        contrato.setLugarTrabajo(dto.getLugarTrabajo());
        contrato.setFechaFinPeriodoPrueba(dto.getFechaFinPeriodoPrueba());
        contrato.setHorasSemanales(dto.getHorasSemanales());
        contrato.setFormaPago(dto.getFormaPago());
        contrato.setPeriodicidadPago(dto.getPeriodicidadPago());

        ContratoEmpleado saved = contratoRepository.save(contrato);
        log.info("Nuevo contrato creado: id={} para empleado id={}",
                saved.getId(), empleado.getId());
        return saved;
    }

    @Override
    public void terminar(Long id, CausaTerminacion causa, LocalDate fechaTerminacion, String descripcion) {
        ContratoEmpleado contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contrato no encontrado: " + id));

        contrato.setActivo(false);
        contrato.setCausaTerminacion(causa);
        contrato.setFechaTerminacion(fechaTerminacion);
        contrato.setDescripcionTerminacion(descripcion);

        contratoRepository.save(contrato);
        log.info("Contrato terminado: id={} causa={}", id, causa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoEmpleado> findByEmpleado(Long empleadoId) {
        return contratoRepository.findByEmpleadoId(empleadoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoEmpleado> findAll() {
        return contratoRepository.findAll();
    }
}
