package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.dto.EmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.EstadoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;
import api.astro.whats_orders_manager.modules.rrhh.repository.DepartamentoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.PuestoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.EmpleadoService;
import api.astro.whats_orders_manager.modules.rrhh.service.SaldoVacacionesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final DepartamentoRepository departamentoRepository;
    private final PuestoRepository puestoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SaldoVacacionesService saldoVacacionesService;

    @Override
    public Empleado crear(EmpleadoDTO dto) {
        // Validate cedula uniqueness
        if (empleadoRepository.findByCedula(dto.getCedula()).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un empleado con la cédula: " + dto.getCedula());
        }

        Empleado empleado = new Empleado();
        mapFromDto(empleado, dto);

        // Set mandatory defaults on creation
        empleado.setEstado(EstadoEmpleado.ACTIVO);
        empleado.setActivo(true);

        Empleado saved = empleadoRepository.save(empleado);
        log.info("Empleado creado: {} (cédula: {})", saved.getNombreCompleto(), saved.getCedula());

        // Initialize vacation balance (Art. 153 Código de Trabajo)
        saldoVacacionesService.inicializar(saved.getId());

        return saved;
    }

    @Override
    public Empleado actualizar(Long id, EmpleadoDTO dto) {
        Empleado empleado = findById(id);
        mapFromDto(empleado, dto);
        Empleado saved = empleadoRepository.save(empleado);
        log.info("Empleado actualizado: {}", saved.getNombreCompleto());
        return saved;
    }

    @Override
    public void darDeBaja(Long id, LocalDate fechaSalida, String motivo) {
        Empleado empleado = findById(id);
        empleado.setActivo(false);
        empleado.setEstado(EstadoEmpleado.BAJA_DEFINITIVA);
        empleado.setFechaSalida(fechaSalida);
        empleado.setMotivoSalida(motivo);
        empleadoRepository.save(empleado);
        log.info("Empleado dado de baja: {} — motivo: {}", empleado.getNombreCompleto(), motivo);
    }

    @Override
    @Transactional(readOnly = true)
    public Empleado findById(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Empleado> findAll(Pageable pageable) {
        return empleadoRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Empleado> findActivos() {
        return empleadoRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActivos() {
        return empleadoRepository.countByActivoTrue();
    }

    @Override
    @Transactional
    public void vincularUsuario(Long empleadoId, Integer usuarioId) {
        // Clear any existing holder of this usuarioId
        empleadoRepository.findByUsuarioId(usuarioId).ifPresent(prior -> {
            prior.setUsuario(null);
            empleadoRepository.save(prior);
        });

        // Link target employee
        Empleado target = findById(empleadoId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + usuarioId));
        target.setUsuario(usuario);
        empleadoRepository.save(target);
        log.info("Empleado {} vinculado a usuario {}", empleadoId, usuarioId);
    }

    @Override
    @Transactional
    public void desvincularUsuario(Integer usuarioId) {
        empleadoRepository.findByUsuarioId(usuarioId).ifPresent(empleado -> {
            empleado.setUsuario(null);
            empleadoRepository.save(empleado);
            log.info("Empleado {} desvinculado de usuario {}", empleado.getId(), usuarioId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> findUsuariosDisponibles(Long excludeEmpleadoId) {
        List<Integer> linkedIds;
        if (excludeEmpleadoId != null) {
            linkedIds = empleadoRepository.findLinkedUsuarioIdsExcluding(excludeEmpleadoId);
        } else {
            linkedIds = empleadoRepository.findAllLinkedUsuarioIds();
        }

        return usuarioRepository.findAll().stream()
                .filter(u -> !linkedIds.contains(u.getIdUsuario()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Empleado> findEmpleadosDisponibles(Integer excludeUsuarioId) {
        List<Empleado> activos = empleadoRepository.findByActivoTrue();

        if (excludeUsuarioId == null) {
            // New user form: exclude all already-linked employees
            return activos.stream()
                    .filter(e -> e.getUsuario() == null)
                    .collect(Collectors.toList());
        }

        // Edit form: include the employee linked to this user (the current value stays selectable)
        // plus all unlinked employees
        java.util.Optional<Empleado> currentHolder = empleadoRepository.findByUsuarioId(excludeUsuarioId);
        Long currentHolderId = currentHolder.map(Empleado::getId).orElse(null);

        return activos.stream()
                .filter(e -> e.getUsuario() == null || e.getId().equals(currentHolderId))
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void mapFromDto(Empleado empleado, EmpleadoDTO dto) {
        empleado.setCedula(dto.getCedula());
        empleado.setNombre(dto.getNombre());
        empleado.setPrimerApellido(dto.getPrimerApellido());
        empleado.setSegundoApellido(dto.getSegundoApellido());
        empleado.setGenero(dto.getGenero());
        empleado.setEstadoCivil(dto.getEstadoCivil());
        empleado.setEmail(dto.getEmail());
        empleado.setTelefono(dto.getTelefono());
        empleado.setDireccion(dto.getDireccion());
        empleado.setFechaNacimiento(dto.getFechaNacimiento());
        empleado.setFechaIngreso(dto.getFechaIngreso());
        empleado.setNumeroAseguradoCCSS(dto.getNumeroAseguradoCCSS());
        empleado.setOperadoraPensionesROP(dto.getOperadoraPensionesROP());
        empleado.setHijosCargaFamiliar(dto.getHijosCargaFamiliar() != null ? dto.getHijosCargaFamiliar() : 0);
        empleado.setConyugeCargaFamiliar(dto.getConyugeCargaFamiliar() != null ? dto.getConyugeCargaFamiliar() : false);
        empleado.setPorcentajeSolidarista(dto.getPorcentajeSolidarista());
        empleado.setMontoPensionAlimentaria(dto.getMontoPensionAlimentaria());
        empleado.setCuentaBancaria(dto.getCuentaBancaria());
        empleado.setBanco(dto.getBanco());
        empleado.setContactoEmergenciaNombre(dto.getContactoEmergenciaNombre());
        empleado.setContactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono());
        empleado.setContactoEmergenciaRelacion(dto.getContactoEmergenciaRelacion());
        empleado.setFotoUrl(dto.getFotoUrl());

        // Resolve Departamento
        if (dto.getDepartamentoId() != null) {
            Departamento dept = departamentoRepository.findById(dto.getDepartamentoId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Departamento no encontrado: " + dto.getDepartamentoId()));
            empleado.setDepartamento(dept);
        }

        // Resolve Puesto
        if (dto.getPuestoId() != null) {
            Puesto puesto = puestoRepository.findById(dto.getPuestoId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Puesto no encontrado: " + dto.getPuestoId()));
            empleado.setPuesto(puesto);
        }

        // Resolve Usuario (nullable — not every employee has an ERP account)
        if (dto.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Usuario no encontrado: " + dto.getUsuarioId()));
            empleado.setUsuario(usuario);
        } else {
            empleado.setUsuario(null);
        }
    }
}
