package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.EmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.EstadoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.enums.Genero;
import api.astro.whats_orders_manager.modules.rrhh.enums.EstadoCivil;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;
import api.astro.whats_orders_manager.modules.rrhh.repository.DepartamentoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.PuestoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.EmpleadoServiceImpl;
import api.astro.whats_orders_manager.modules.rrhh.service.SaldoVacacionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmpleadoServiceImpl.
 *
 * RED phase — written before implementation exists.
 *
 * Covers:
 * PR3.5 — crear() rejects duplicate cedula
 * PR3.6 — crear() succeeds when usuario is null (nullable FK)
 * PR3.7 — darDeBaja() sets estado=BAJA_DEFINITIVA, activo=false, fechaSalida
 * PR3.8 — findById() throws when not found
 * PR3.9 — findActivos() delegates to repository
 * PR3.10 — countActivos() delegates to repository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmpleadoServiceImpl — unit tests")
class EmpleadoServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private PuestoRepository puestoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SaldoVacacionesService saldoVacacionesService;

    private EmpleadoService service;

    private Departamento departamento;
    private Puesto puesto;

    @BeforeEach
    void setUp() {
        service = new EmpleadoServiceImpl(
                empleadoRepository,
                departamentoRepository,
                puestoRepository,
                usuarioRepository,
                saldoVacacionesService
        );

        departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNombre("Administración");
        departamento.setActivo(true);

        puesto = new Puesto();
        puesto.setId(1L);
        puesto.setNombre("Asistente");
        puesto.setSalarioBase(new BigDecimal("400000.00"));
        puesto.setActivo(true);
        puesto.setDepartamento(departamento);
    }

    // =========================================================================
    // PR3.5 — crear() rejects duplicate cedula
    // =========================================================================

    @Test
    @DisplayName("crear() must reject duplicate cedula with IllegalArgumentException")
    void crear_duplicateCedula_throwsIllegalArgument() {
        // GIVEN empleado with cedula "123456789" already exists
        EmpleadoDTO dto = buildDTO("123456789", null);

        when(empleadoRepository.findByCedula("123456789"))
                .thenReturn(Optional.of(new Empleado()));

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(dto)
        );

        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("cedula") || msg.contains("cédula"),
                "Error message must reference 'cedula'. Got: " + ex.getMessage());
        verify(empleadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() must reject a second distinct duplicate cedula with IllegalArgumentException")
    void crear_otraCedulaDuplicada_throwsIllegalArgument() {
        // GIVEN employee with cedula "987000001" already exists
        EmpleadoDTO dto = buildDTO("987000001", null);

        when(empleadoRepository.findByCedula("987000001"))
                .thenReturn(Optional.of(new Empleado()));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(dto)
        );

        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("cedula") || msg.contains("cédula"),
                "Error message must reference 'cedula'. Got: " + ex.getMessage());
        verify(empleadoRepository, never()).save(any());
    }

    // =========================================================================
    // PR3.6 — crear() succeeds when usuario is null (optional FK)
    // =========================================================================

    @Test
    @DisplayName("crear() must succeed when usuarioId is null — usuario FK is optional")
    void crear_usuarioNull_persistsSuccessfully() {
        // GIVEN no usuario linked
        EmpleadoDTO dto = buildDTO("987654321", null); // null usuarioId

        Empleado saved = buildEmpleado("987654321");
        saved.setId(1L);
        saved.setUsuario(null);

        when(empleadoRepository.findByCedula("987654321")).thenReturn(Optional.empty());
        when(departamentoRepository.findById(1L)).thenReturn(Optional.of(departamento));
        when(puestoRepository.findById(1L)).thenReturn(Optional.of(puesto));
        when(empleadoRepository.save(any(Empleado.class))).thenReturn(saved);

        // WHEN
        Empleado result = service.crear(dto);

        // THEN — must persist without error, usuario is null
        assertNotNull(result);
        assertNull(result.getUsuario(), "usuario must be null when no usuarioId provided");
        assertEquals(EstadoEmpleado.ACTIVO, result.getEstado());
        assertTrue(result.getActivo());
        verify(empleadoRepository).save(any(Empleado.class));
    }

    // =========================================================================
    // PR3.7 — darDeBaja() sets all three required fields
    // =========================================================================

    @Test
    @DisplayName("darDeBaja() must set estado=BAJA_DEFINITIVA, activo=false, and fechaSalida")
    void darDeBaja_activoEmpleado_setsAllThreeFields() {
        // GIVEN active employee
        Long id = 5L;
        Empleado empleado = buildEmpleado("111111111");
        empleado.setId(id);
        empleado.setActivo(true);
        empleado.setEstado(EstadoEmpleado.ACTIVO);
        empleado.setFechaSalida(null);

        LocalDate fechaBaja = LocalDate.of(2026, 7, 21);
        String motivo = "Renuncia voluntaria";

        when(empleadoRepository.findById(id)).thenReturn(Optional.of(empleado));
        when(empleadoRepository.save(empleado)).thenReturn(empleado);

        // WHEN
        service.darDeBaja(id, fechaBaja, motivo);

        // THEN — all three fields must be set
        assertFalse(empleado.getActivo(), "activo must be false after darDeBaja");
        assertEquals(EstadoEmpleado.BAJA_DEFINITIVA, empleado.getEstado(),
                "estado must be BAJA_DEFINITIVA");
        assertEquals(fechaBaja, empleado.getFechaSalida(),
                "fechaSalida must match the provided date");
        assertEquals(motivo, empleado.getMotivoSalida(),
                "motivoSalida must match the provided motivo");
        verify(empleadoRepository).save(empleado);
    }

    @Test
    @DisplayName("darDeBaja() must set estado=BAJA_DEFINITIVA for a second employee with different data")
    void darDeBaja_segundoEmpleadoDiferenteMotivo_setsAllThreeFields() {
        Long id = 8L;
        Empleado empleado2 = buildEmpleado("444444444");
        empleado2.setId(id);
        empleado2.setActivo(true);
        empleado2.setEstado(EstadoEmpleado.ACTIVO);
        empleado2.setFechaSalida(null);

        LocalDate fechaBaja = LocalDate.of(2026, 3, 31);
        String motivo = "Pensión anticipada";

        when(empleadoRepository.findById(id)).thenReturn(Optional.of(empleado2));
        when(empleadoRepository.save(empleado2)).thenReturn(empleado2);

        service.darDeBaja(id, fechaBaja, motivo);

        assertFalse(empleado2.getActivo());
        assertEquals(EstadoEmpleado.BAJA_DEFINITIVA, empleado2.getEstado());
        assertEquals(fechaBaja, empleado2.getFechaSalida());
        assertEquals(motivo, empleado2.getMotivoSalida());
        verify(empleadoRepository).save(empleado2);
    }

    // =========================================================================
    // PR3.8 — findById() throws when not found
    // =========================================================================

    @Test
    @DisplayName("findById() must throw NoSuchElementException when empleado not found")
    void findById_notFound_throws() {
        when(empleadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.findById(99L));
    }

    // =========================================================================
    // PR3.9 — findActivos() delegates to repository
    // =========================================================================

    @Test
    @DisplayName("findActivos() must return all active employees from repository")
    void findActivos_delegatesToRepository() {
        Empleado e = buildEmpleado("222222222");
        e.setActivo(true);

        when(empleadoRepository.findByActivoTrue()).thenReturn(List.of(e));

        List<Empleado> result = service.findActivos();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getActivo());
        verify(empleadoRepository).findByActivoTrue();
    }

    @Test
    @DisplayName("findActivos() must return multiple employees when repository has several active")
    void findActivos_variosActivos_returnsAll() {
        Empleado e1 = buildEmpleado("555555555");
        e1.setActivo(true);
        Empleado e2 = buildEmpleado("666666666");
        e2.setActivo(true);

        when(empleadoRepository.findByActivoTrue()).thenReturn(List.of(e1, e2));

        List<Empleado> result = service.findActivos();

        assertEquals(2, result.size());
        result.forEach(e -> assertTrue(e.getActivo()));
        verify(empleadoRepository).findByActivoTrue();
    }

    // =========================================================================
    // PR3.10 — countActivos() delegates to repository
    // =========================================================================

    @Test
    @DisplayName("countActivos() must return the count of active employees")
    void countActivos_returnsCount() {
        when(empleadoRepository.countByActivoTrue()).thenReturn(42L);

        long count = service.countActivos();

        assertEquals(42L, count);
        verify(empleadoRepository).countByActivoTrue();
    }

    @Test
    @DisplayName("countActivos() must return a different count when repository returns a distinct value")
    void countActivos_conteoDistinto_retornaValorCorrecto() {
        when(empleadoRepository.countByActivoTrue()).thenReturn(7L);

        long count = service.countActivos();

        assertEquals(7L, count);
        verify(empleadoRepository).countByActivoTrue();
    }

    // =========================================================================
    // Regression: findAll(Pageable) delegates to repository
    // =========================================================================

    @Test
    @DisplayName("findAll(Pageable) must return paginated results from repository")
    void findAll_pageable_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Empleado e = buildEmpleado("333333333");
        Page<Empleado> page = new PageImpl<>(List.of(e));

        when(empleadoRepository.findAll(pageable)).thenReturn(page);

        Page<Empleado> result = service.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        verify(empleadoRepository).findAll(pageable);
    }

    // =========================================================================
    // G2.6 — crear() must call saldoVacacionesService.inicializar() after save
    // =========================================================================

    @Test
    @DisplayName("crear() must call saldoVacacionesService.inicializar() after persisting the employee")
    void crear_empleado_inicializaSaldoVacaciones() {
        // GIVEN
        EmpleadoDTO dto = buildDTO("111222333", null);

        Empleado saved = buildEmpleado("111222333");
        saved.setId(20L);
        saved.setUsuario(null);

        when(empleadoRepository.findByCedula("111222333")).thenReturn(Optional.empty());
        when(departamentoRepository.findById(1L)).thenReturn(Optional.of(departamento));
        when(puestoRepository.findById(1L)).thenReturn(Optional.of(puesto));
        when(empleadoRepository.save(any(Empleado.class))).thenReturn(saved);

        // WHEN
        service.crear(dto);

        // THEN — saldoVacacionesService.inicializar() must be called with the saved employee id
        verify(saldoVacacionesService).inicializar(20L);
    }

    // =========================================================================
    // PR2 — vincularUsuarioClearsPreviousLink
    // =========================================================================

    @Test
    @DisplayName("vincularUsuario() must clear previous link before linking to target employee")
    void vincularUsuarioClearsPreviousLink() {
        // GIVEN employee A is already linked to user X
        Usuario userX = buildUsuario(10);
        Empleado empleadoA = buildEmpleado("111000001");
        empleadoA.setId(1L);
        empleadoA.setUsuario(userX);

        Empleado empleadoB = buildEmpleado("222000002");
        empleadoB.setId(2L);
        empleadoB.setUsuario(null);

        // When vincularUsuario(empleadoB.id, userX.idUsuario) is called
        // — first clear A, then set B
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleadoA));
        when(empleadoRepository.findById(2L)).thenReturn(Optional.of(empleadoB));
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(userX));

        // WHEN
        service.vincularUsuario(2L, 10);

        // THEN — A's usuario must be cleared, B's usuario must be set to userX
        assertNull(empleadoA.getUsuario(), "Previous holder (empleadoA) must have usuario cleared");
        assertEquals(userX, empleadoB.getUsuario(), "Target (empleadoB) must have usuario set to userX");
        verify(empleadoRepository, times(2)).save(any(Empleado.class));
    }

    @Test
    @DisplayName("vincularUsuario() must link employee when no prior holder exists")
    void vincularUsuarioNoPreviousHolder() {
        // GIVEN no employee holds userX
        Usuario userX = buildUsuario(20);
        Empleado empleadoA = buildEmpleado("333000003");
        empleadoA.setId(3L);
        empleadoA.setUsuario(null);

        when(empleadoRepository.findByUsuarioId(20)).thenReturn(Optional.empty());
        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleadoA));
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(userX));

        // WHEN
        service.vincularUsuario(3L, 20);

        // THEN — empleadoA must be linked to userX
        assertEquals(userX, empleadoA.getUsuario());
        verify(empleadoRepository, times(1)).save(any(Empleado.class));
    }

    // =========================================================================
    // PR2 — desvincularUsuarioSetsNull
    // =========================================================================

    @Test
    @DisplayName("desvincularUsuario() must set employee usuario to null and save")
    void desvincularUsuarioSetsNull() {
        // GIVEN employee A is linked to user X
        Usuario userX = buildUsuario(30);
        Empleado empleadoA = buildEmpleado("444000004");
        empleadoA.setId(4L);
        empleadoA.setUsuario(userX);

        when(empleadoRepository.findByUsuarioId(30)).thenReturn(Optional.of(empleadoA));
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        service.desvincularUsuario(30);

        // THEN — employee usuario must be null
        assertNull(empleadoA.getUsuario(), "Empleado usuario must be null after desvincularUsuario");
        verify(empleadoRepository).save(empleadoA);
    }

    @Test
    @DisplayName("desvincularUsuario() must do nothing when no employee holds that usuario")
    void desvincularUsuarioNoOp() {
        when(empleadoRepository.findByUsuarioId(99)).thenReturn(Optional.empty());

        // WHEN — should not throw
        service.desvincularUsuario(99);

        // THEN — save never called
        verify(empleadoRepository, never()).save(any());
    }

    // =========================================================================
    // PR2 — findUsuariosDisponiblesExcludesLinked
    // =========================================================================

    @Test
    @DisplayName("findUsuariosDisponibles() must exclude users linked to other employees")
    void findUsuariosDisponiblesExcludesLinked() {
        // GIVEN user X (id=10) is linked to empleadoA (id=1), user Y (id=20) is free
        // When called with excludeEmpleadoId=1 (editing empleadoA),
        // user X should still appear (it's the current employee's own user)
        // and user Y should appear too.
        Usuario userX = buildUsuario(10);
        Usuario userY = buildUsuario(20);

        // linked IDs excluding empleadoA: only those linked to OTHER employees
        when(empleadoRepository.findLinkedUsuarioIdsExcluding(1L)).thenReturn(List.of());
        when(usuarioRepository.findAll()).thenReturn(List.of(userX, userY));

        // WHEN
        List<Usuario> result = service.findUsuariosDisponibles(1L);

        // THEN — both users should appear (neither is linked to another employee)
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findUsuariosDisponibles() must exclude users linked to OTHER employees (not the edited one)")
    void findUsuariosDisponiblesExcludesOtherLinked() {
        // GIVEN user X (id=10) is linked to empleadoB (id=2)
        // When editing empleadoA (id=1), user X should NOT appear
        Usuario userX = buildUsuario(10);
        Usuario userY = buildUsuario(20);

        when(empleadoRepository.findLinkedUsuarioIdsExcluding(1L)).thenReturn(List.of(10));
        when(usuarioRepository.findAll()).thenReturn(List.of(userX, userY));

        // WHEN
        List<Usuario> result = service.findUsuariosDisponibles(1L);

        // THEN — only user Y should appear
        assertEquals(1, result.size());
        assertEquals(20, result.get(0).getIdUsuario());
    }

    @Test
    @DisplayName("findUsuariosDisponibles(null) must exclude all linked users (new employee form)")
    void findUsuariosDisponiblesNullExclude() {
        // GIVEN user X (id=10) is linked to some employee
        Usuario userX = buildUsuario(10);
        Usuario userY = buildUsuario(20);

        when(empleadoRepository.findAllLinkedUsuarioIds()).thenReturn(List.of(10));
        when(usuarioRepository.findAll()).thenReturn(List.of(userX, userY));

        // WHEN
        List<Usuario> result = service.findUsuariosDisponibles(null);

        // THEN — only free user Y should appear
        assertEquals(1, result.size());
        assertEquals(20, result.get(0).getIdUsuario());
    }

    // =========================================================================
    // PR2 — findEmpleadosDisponibles (for UsuarioAdminController)
    // =========================================================================

    @Test
    @DisplayName("findEmpleadosDisponibles() must exclude employees linked to other users")
    void findEmpleadosDisponiblesExcludesLinked() {
        // GIVEN empleadoA (id=1) is linked to userX (id=10)
        // empleadoB (id=2) is free
        // When called with excludeUsuarioId=10 (editing userX), empleadoA should still appear
        // (it's the current user's own employee). empleadoB should also appear.
        Usuario userX = buildUsuario(10);
        Empleado empleadoA = buildEmpleado("500000001");
        empleadoA.setId(1L);
        empleadoA.setUsuario(userX);

        Empleado empleadoB = buildEmpleado("500000002");
        empleadoB.setId(2L);
        empleadoB.setUsuario(null);

        when(empleadoRepository.findByActivoTrue()).thenReturn(List.of(empleadoA, empleadoB));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleadoA));

        // WHEN
        List<Empleado> result = service.findEmpleadosDisponibles(10);

        // THEN — both A and B appear (A is the current user's employee, B is free)
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findEmpleadosDisponibles(null) must exclude all already-linked employees")
    void findEmpleadosDisponiblesNullExclude() {
        // GIVEN empleadoA linked to userX, empleadoB free
        // When called with null, empleadoA should NOT appear
        Usuario userX = buildUsuario(10);
        Empleado empleadoA = buildEmpleado("600000001");
        empleadoA.setId(1L);
        empleadoA.setUsuario(userX);

        Empleado empleadoB = buildEmpleado("600000002");
        empleadoB.setId(2L);
        empleadoB.setUsuario(null);

        when(empleadoRepository.findByActivoTrue()).thenReturn(List.of(empleadoA, empleadoB));

        // WHEN
        List<Empleado> result = service.findEmpleadosDisponibles(null);

        // THEN — only empleadoB should appear (empleadoA is already linked)
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Usuario buildUsuario(Integer id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre("Usuario " + id);
        u.setEmail("user" + id + "@test.com");
        u.setActivo(true);
        return u;
    }

    private EmpleadoDTO buildDTO(String cedula, Integer usuarioId) {
        EmpleadoDTO dto = new EmpleadoDTO();
        dto.setCedula(cedula);
        dto.setNombre("Juan");
        dto.setPrimerApellido("Pérez");
        dto.setSegundoApellido("Rodríguez");
        dto.setFechaIngreso(LocalDate.of(2024, 1, 15));
        dto.setDepartamentoId(1L);
        dto.setPuestoId(1L);
        dto.setUsuarioId(usuarioId);
        return dto;
    }

    private Empleado buildEmpleado(String cedula) {
        Empleado e = new Empleado();
        e.setCedula(cedula);
        e.setNombre("Juan");
        e.setPrimerApellido("Pérez");
        e.setFechaIngreso(LocalDate.of(2024, 1, 15));
        e.setEstado(EstadoEmpleado.ACTIVO);
        e.setActivo(true);
        e.setHijosCargaFamiliar(0);
        e.setConyugeCargaFamiliar(false);
        e.setDepartamento(departamento);
        e.setPuesto(puesto);
        return e;
    }
}
