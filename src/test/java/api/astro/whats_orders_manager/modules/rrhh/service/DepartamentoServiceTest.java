package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.DepartamentoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.DepartamentoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.PuestoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.DepartamentoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DepartamentoServiceImpl.
 *
 * RED phase — written before implementation.
 *
 * Covers:
 * - crear() rejects duplicate nombre
 * - actualizar() rejects duplicate nombre on different record
 * - desactivar() is blocked when active puestos reference the department
 * - desactivar() succeeds when no active puestos exist
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartamentoServiceImpl — unit tests")
class DepartamentoServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private PuestoRepository puestoRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    private DepartamentoService service;

    @BeforeEach
    void setUp() {
        service = new DepartamentoServiceImpl(departamentoRepository, puestoRepository, empleadoRepository);
    }

    // =========================================================================
    // crear() — nombre uniqueness
    // =========================================================================

    @Test
    @DisplayName("crear() must reject duplicate nombre")
    void crear_duplicateNombre_throwsIllegalArgument() {
        // GIVEN a departamento named "Ventas" already exists
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("Ventas");

        when(departamentoRepository.existsByNombreAndIdNot("Ventas", 0L)).thenReturn(true);

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(dto)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("nombre"),
                "Error message must mention 'nombre'");
        verify(departamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() must persist when nombre is unique")
    void crear_uniqueNombre_persists() {
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("Logística");

        Departamento saved = new Departamento();
        saved.setId(1L);
        saved.setNombre("Logística");
        saved.setActivo(true);

        when(departamentoRepository.existsByNombreAndIdNot("Logística", 0L)).thenReturn(false);
        when(departamentoRepository.save(any(Departamento.class))).thenReturn(saved);

        Departamento result = service.crear(dto);

        assertNotNull(result);
        assertEquals("Logística", result.getNombre());
        verify(departamentoRepository).save(any(Departamento.class));
    }

    @Test
    @DisplayName("crear() must persist a second different departamento with distinct nombre")
    void crear_segundoDepartamentoDiferenteNombre_persists() {
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("Contabilidad");

        Departamento saved = new Departamento();
        saved.setId(2L);
        saved.setNombre("Contabilidad");
        saved.setActivo(true);

        when(departamentoRepository.existsByNombreAndIdNot("Contabilidad", 0L)).thenReturn(false);
        when(departamentoRepository.save(any(Departamento.class))).thenReturn(saved);

        Departamento result = service.crear(dto);

        assertNotNull(result);
        assertEquals("Contabilidad", result.getNombre());
        assertTrue(result.isActivo());
        verify(departamentoRepository).save(any(Departamento.class));
    }

    // =========================================================================
    // actualizar() — nombre uniqueness on different record
    // =========================================================================

    @Test
    @DisplayName("actualizar() must reject nombre already used by another record")
    void actualizar_duplicateNombreOtherRecord_throwsIllegalArgument() {
        Long id = 2L;
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("Ventas");

        Departamento existing = new Departamento();
        existing.setId(id);
        existing.setNombre("Marketing");
        existing.setActivo(true);

        when(departamentoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departamentoRepository.existsByNombreAndIdNot("Ventas", id)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(id, dto));
        verify(departamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar() must persist when nombre is unique across other records")
    void actualizar_nombreUnico_persists() {
        Long id = 4L;
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("Finanzas");

        Departamento existing = new Departamento();
        existing.setId(id);
        existing.setNombre("Contabilidad");
        existing.setActivo(true);

        Departamento saved = new Departamento();
        saved.setId(id);
        saved.setNombre("Finanzas");
        saved.setActivo(true);

        when(departamentoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departamentoRepository.existsByNombreAndIdNot("Finanzas", id)).thenReturn(false);
        when(departamentoRepository.save(any(Departamento.class))).thenReturn(saved);

        Departamento result = service.actualizar(id, dto);

        assertNotNull(result);
        assertEquals("Finanzas", result.getNombre());
        verify(departamentoRepository).save(any(Departamento.class));
    }

    @Test
    @DisplayName("actualizar() must reject duplicate nombre even for a different pair of records")
    void actualizar_otroPar_duplicateNombre_throwsIllegalArgument() {
        Long id = 7L;
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("RRHH");

        Departamento existing = new Departamento();
        existing.setId(id);
        existing.setNombre("Sistemas");
        existing.setActivo(true);

        when(departamentoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departamentoRepository.existsByNombreAndIdNot("RRHH", id)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(id, dto));
        verify(departamentoRepository, never()).save(any());
    }

    // =========================================================================
    // desactivar() — blocked by active puestos
    // =========================================================================

    @Test
    @DisplayName("desactivar() must throw when departamento has active puestos")
    void desactivar_withActivePuestos_throwsIllegalState() {
        // GIVEN departamento "TI" has active puestos
        Long id = 1L;
        Departamento dept = new Departamento();
        dept.setId(id);
        dept.setNombre("TI");
        dept.setActivo(true);

        when(departamentoRepository.findById(id)).thenReturn(Optional.of(dept));
        when(puestoRepository.existsByActivoTrueAndDepartamento(dept)).thenReturn(true);

        // WHEN / THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.desactivar(id)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("puesto") ||
                   ex.getMessage().toLowerCase().contains("activo"),
                "Error message must mention active puestos");

        // departamento must remain active
        assertTrue(dept.isActivo(), "Departamento must remain active after failed desactivar");
        verify(departamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivar() must succeed when no active puestos")
    void desactivar_noActivePuestos_setsActivoFalse() {
        Long id = 3L;
        Departamento dept = new Departamento();
        dept.setId(id);
        dept.setNombre("Archivos");
        dept.setActivo(true);

        when(departamentoRepository.findById(id)).thenReturn(Optional.of(dept));
        when(puestoRepository.existsByActivoTrueAndDepartamento(dept)).thenReturn(false);
        when(departamentoRepository.save(dept)).thenReturn(dept);

        service.desactivar(id);

        assertFalse(dept.isActivo(), "Departamento must be deactivated");
        verify(departamentoRepository).save(dept);
    }

    @Test
    @DisplayName("desactivar() must throw when departamento not found")
    void desactivar_notFound_throwsNoSuchElement() {
        when(departamentoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.desactivar(99L));
    }

    // =========================================================================
    // findAll / findActivos
    // =========================================================================

    @Test
    @DisplayName("findAll() must delegate to repository")
    void findAll_delegatesToRepository() {
        Departamento d = new Departamento();
        d.setNombre("RRHH");
        when(departamentoRepository.findAll()).thenReturn(List.of(d));

        List<Departamento> result = service.findAll();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findAll() must return multiple departamentos when repository has several")
    void findAll_multiplesRegistros_returnsAll() {
        Departamento d1 = new Departamento();
        d1.setNombre("TI");
        d1.setActivo(true);
        Departamento d2 = new Departamento();
        d2.setNombre("Ventas");
        d2.setActivo(false);

        when(departamentoRepository.findAll()).thenReturn(List.of(d1, d2));

        List<Departamento> result = service.findAll();

        assertEquals(2, result.size());
        verify(departamentoRepository).findAll();
    }

    @Test
    @DisplayName("findActivos() must return only active departments")
    void findActivos_returnsOnlyActive() {
        Departamento d = new Departamento();
        d.setNombre("TI");
        d.setActivo(true);
        when(departamentoRepository.findByActivoTrue()).thenReturn(List.of(d));

        List<Departamento> result = service.findActivos();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActivo());
    }

    @Test
    @DisplayName("findActivos() must return multiple active departments when several exist")
    void findActivos_variosActivos_returnsAll() {
        Departamento d1 = new Departamento();
        d1.setNombre("Finanzas");
        d1.setActivo(true);
        Departamento d2 = new Departamento();
        d2.setNombre("Mercadeo");
        d2.setActivo(true);

        when(departamentoRepository.findByActivoTrue()).thenReturn(List.of(d1, d2));

        List<Departamento> result = service.findActivos();

        assertEquals(2, result.size());
        result.forEach(r -> assertTrue(r.isActivo()));
        verify(departamentoRepository).findByActivoTrue();
    }

    @Test
    @DisplayName("desactivar() must also work for a different departamento with a distinct id")
    void desactivar_diferenteDepartamento_setsActivoFalse() {
        Long id = 9L;
        Departamento dept = new Departamento();
        dept.setId(id);
        dept.setNombre("Legal");
        dept.setActivo(true);

        when(departamentoRepository.findById(id)).thenReturn(Optional.of(dept));
        when(puestoRepository.existsByActivoTrueAndDepartamento(dept)).thenReturn(false);
        when(departamentoRepository.save(dept)).thenReturn(dept);

        service.desactivar(id);

        assertFalse(dept.isActivo(), "Departamento 'Legal' must be deactivated");
        verify(departamentoRepository).save(dept);
    }

    // =========================================================================
    // actualizar() — jefe assignment (Phase 2 RED tests)
    // =========================================================================

    @Test
    @DisplayName("actualizar() must set jefe when jefeId is provided")
    void actualizarSetsJefe() {
        Long deptId = 5L;
        Long jefeId = 10L;

        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setNombre("TI");
        dept.setActivo(true);

        Empleado jefe = new Empleado();
        jefe.setId(jefeId);
        jefe.setNombre("Ana");
        jefe.setPrimerApellido("García");

        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("TI");
        dto.setJefeId(jefeId);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(departamentoRepository.existsByNombreAndIdNot("TI", deptId)).thenReturn(false);
        when(empleadoRepository.findById(jefeId)).thenReturn(Optional.of(jefe));
        when(departamentoRepository.save(any(Departamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Departamento result = service.actualizar(deptId, dto);

        assertNotNull(result.getJefe(), "Jefe must be set after actualizar with jefeId");
        assertEquals(jefeId, result.getJefe().getId(), "Jefe id must match requested jefeId");
        verify(empleadoRepository).findById(jefeId);
        verify(departamentoRepository).save(dept);
    }

    @Test
    @DisplayName("actualizar() must clear jefe when jefeId is null")
    void actualizarClearsJefeWhenNull() {
        Long deptId = 6L;

        Empleado currentJefe = new Empleado();
        currentJefe.setId(20L);
        currentJefe.setNombre("Carlos");
        currentJefe.setPrimerApellido("López");

        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setNombre("Ventas");
        dept.setActivo(true);
        dept.setJefe(currentJefe);

        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setNombre("Ventas");
        dto.setJefeId(null);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(departamentoRepository.existsByNombreAndIdNot("Ventas", deptId)).thenReturn(false);
        when(departamentoRepository.save(any(Departamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Departamento result = service.actualizar(deptId, dto);

        assertNull(result.getJefe(), "Jefe must be null after actualizar with jefeId=null");
        verify(empleadoRepository, never()).findById(any());
        verify(departamentoRepository).save(dept);
    }
}
