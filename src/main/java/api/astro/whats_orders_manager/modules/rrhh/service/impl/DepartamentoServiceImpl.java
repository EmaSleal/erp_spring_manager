package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.dto.DepartamentoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.DepartamentoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.PuestoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.DepartamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DepartamentoServiceImpl implements DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final PuestoRepository puestoRepository;
    private final EmpleadoRepository empleadoRepository;

    @Override
    public Departamento crear(DepartamentoDTO dto) {
        if (departamentoRepository.existsByNombreAndIdNot(dto.getNombre(), 0L)) {
            throw new IllegalArgumentException(
                    "Ya existe un departamento con el nombre: " + dto.getNombre());
        }
        Departamento dept = new Departamento();
        dept.setNombre(dto.getNombre());
        dept.setActivo(true);
        Departamento saved = departamentoRepository.save(dept);
        log.info("Departamento creado: {}", saved.getNombre());
        return saved;
    }

    @Override
    public Departamento actualizar(Long id, DepartamentoDTO dto) {
        Departamento dept = findById(id);
        if (departamentoRepository.existsByNombreAndIdNot(dto.getNombre(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe un departamento con el nombre: " + dto.getNombre());
        }
        dept.setNombre(dto.getNombre());
        dept.setJefe(resolveJefe(dto.getJefeId()));
        Departamento saved = departamentoRepository.save(dept);
        log.info("Departamento actualizado: {}", saved.getNombre());
        return saved;
    }

    private Empleado resolveJefe(Long jefeId) {
        if (jefeId == null) {
            return null;
        }
        return empleadoRepository.findById(jefeId).orElse(null);
    }

    @Override
    public void desactivar(Long id) {
        Departamento dept = findById(id);
        if (puestoRepository.existsByActivoTrueAndDepartamento(dept)) {
            throw new IllegalStateException(
                    "No se puede desactivar el departamento '" + dept.getNombre() +
                    "' porque tiene puestos activos.");
        }
        dept.setActivo(false);
        departamentoRepository.save(dept);
        log.info("Departamento desactivado: {}", dept.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Departamento> findAll() {
        return departamentoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Departamento> findActivos() {
        return departamentoRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Departamento findById(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Departamento no encontrado: " + id));
    }
}
