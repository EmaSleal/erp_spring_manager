package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.dto.PuestoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;
import api.astro.whats_orders_manager.modules.rrhh.model.SalarioMinimo;
import api.astro.whats_orders_manager.modules.rrhh.repository.DepartamentoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.PuestoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.PuestoService;
import api.astro.whats_orders_manager.modules.rrhh.service.SalarioMinimoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PuestoServiceImpl implements PuestoService {

    private final PuestoRepository puestoRepository;
    private final DepartamentoRepository departamentoRepository;
    private final SalarioMinimoService salarioMinimoService;

    @Override
    public Puesto crear(PuestoDTO dto) {
        Departamento dept = requireActiveDepartamento(dto.getDepartamentoId());

        validateMinWage(dto.getNombre(), dto.getCategoriaSalarialMinima(), dto.getSalarioBase());

        Puesto puesto = new Puesto();
        mapFromDto(puesto, dto, dept);
        Puesto saved = puestoRepository.save(puesto);
        log.info("Puesto creado: {} en departamento {}", saved.getNombre(), dept.getNombre());
        return saved;
    }

    @Override
    public Puesto actualizar(Long id, PuestoDTO dto) {
        Puesto puesto = findById(id);
        Departamento dept = requireActiveDepartamento(dto.getDepartamentoId());

        validateMinWage(dto.getNombre(), dto.getCategoriaSalarialMinima(), dto.getSalarioBase());

        mapFromDto(puesto, dto, dept);
        Puesto saved = puestoRepository.save(puesto);
        log.info("Puesto actualizado: {}", saved.getNombre());
        return saved;
    }

    @Override
    public void desactivar(Long id) {
        Puesto puesto = findById(id);
        puesto.setActivo(false);
        puestoRepository.save(puesto);
        log.info("Puesto desactivado: {}", puesto.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Puesto> findAll() {
        return puestoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Puesto> findActivos() {
        return puestoRepository.findAll().stream()
                .filter(Puesto::isActivo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Puesto findById(Long id) {
        return puestoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Puesto no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Puesto> findByDepartamento(Long departamentoId) {
        departamentoRepository.findById(departamentoId)
                .orElseThrow(() -> new NoSuchElementException("Departamento no encontrado: " + departamentoId));
        return puestoRepository.findByDepartamentoIdAndActivoTrue(departamentoId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates salarioBase >= SalarioMinimo.montoMensual for the given categoria.
     * If categoriaSalarialMinima is null, the check is skipped silently.
     */
    private void validateMinWage(String nombrePuesto, String categoria, BigDecimal salarioBase) {
        if (categoria == null) {
            log.debug("Puesto '{}': categoriaSalarialMinima is null — min-wage check skipped.", nombrePuesto);
            return;
        }

        LocalDate today = LocalDate.now();
        SalarioMinimo vigente = salarioMinimoService.findVigenteByCategoria(categoria, today);

        if (salarioBase == null || salarioBase.compareTo(vigente.getMontoMensual()) < 0) {
            BigDecimal required = vigente.getMontoMensual();
            throw new IllegalArgumentException(
                    "El salario base (" + salarioBase + ") del puesto '" + nombrePuesto +
                    "' es inferior al salario mínimo vigente para la categoría '" + categoria +
                    "': ₡" + required + ". Decreto MTSS vigente desde " + vigente.getVigenciaDesde() + ".");
        }

        log.debug("Puesto '{}': salario mínimo validado OK (base={}, mínimo={}).",
                nombrePuesto, salarioBase, vigente.getMontoMensual());
    }

    private Departamento requireActiveDepartamento(Long deptId) {
        Departamento dept = departamentoRepository.findById(deptId)
                .orElseThrow(() -> new NoSuchElementException("Departamento no encontrado: " + deptId));
        if (!dept.isActivo()) {
            throw new IllegalStateException(
                    "El departamento '" + dept.getNombre() + "' está inactivo. " +
                    "Un puesto debe pertenecer a un departamento activo.");
        }
        return dept;
    }

    private void mapFromDto(Puesto puesto, PuestoDTO dto, Departamento dept) {
        puesto.setNombre(dto.getNombre());
        puesto.setDescripcion(dto.getDescripcion());
        puesto.setSalarioBase(dto.getSalarioBase() != null ? dto.getSalarioBase() : BigDecimal.ZERO);
        puesto.setCategoriaSalarialMinima(dto.getCategoriaSalarialMinima());
        puesto.setTipoJornada(dto.getTipoJornada());
        puesto.setDepartamento(dept);
        puesto.setActivo(true);
    }
}
