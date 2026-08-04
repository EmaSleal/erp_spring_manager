package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.PuestoDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;
import api.astro.whats_orders_manager.modules.rrhh.model.SalarioMinimo;
import api.astro.whats_orders_manager.modules.rrhh.repository.DepartamentoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.PuestoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.PuestoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PuestoServiceImpl.
 *
 * RED phase — written before implementation.
 *
 * Covers:
 * - crear() requires an active Departamento
 * - crear() with null categoriaSalarialMinima skips min-wage check (PR1 scaffold)
 * - desactivar() sets activo=false
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PuestoServiceImpl — unit tests")
class PuestoServiceTest {

    @Mock
    private PuestoRepository puestoRepository;

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private SalarioMinimoService salarioMinimoService;

    private PuestoService service;

    @BeforeEach
    void setUp() {
        service = new PuestoServiceImpl(puestoRepository, departamentoRepository, salarioMinimoService);
    }

    // =========================================================================
    // crear() — departamento must exist and be active
    // =========================================================================

    @Test
    @DisplayName("crear() must throw when departamento does not exist")
    void crear_departamentoNotFound_throwsException() {
        PuestoDTO dto = new PuestoDTO();
        dto.setNombre("Analista");
        dto.setDepartamentoId(99L);
        dto.setSalarioBase(BigDecimal.valueOf(500000));

        when(departamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.crear(dto));
        verify(puestoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() must throw when departamento is inactive")
    void crear_departamentoInactive_throwsIllegalState() {
        Long deptId = 1L;
        PuestoDTO dto = new PuestoDTO();
        dto.setNombre("Analista");
        dto.setDepartamentoId(deptId);
        dto.setSalarioBase(BigDecimal.valueOf(500000));

        Departamento inactiveDept = new Departamento();
        inactiveDept.setId(deptId);
        inactiveDept.setNombre("TI");
        inactiveDept.setActivo(false);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(inactiveDept));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.crear(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("activo") ||
                   ex.getMessage().toLowerCase().contains("inactivo"),
                "Error must mention inactive departamento");
        verify(puestoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() with null categoriaSalarialMinima must skip min-wage check and persist")
    void crear_nullCategoriaSalarial_skipsMinWageCheckAndPersists() {
        Long deptId = 1L;
        PuestoDTO dto = new PuestoDTO();
        dto.setNombre("Operario");
        dto.setDepartamentoId(deptId);
        dto.setSalarioBase(BigDecimal.valueOf(300000));
        dto.setCategoriaSalarialMinima(null); // min-wage check skipped
        dto.setTipoJornada(TipoJornada.DIURNA);

        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setNombre("Operaciones");
        dept.setActivo(true);

        Puesto saved = new Puesto();
        saved.setId(10L);
        saved.setNombre("Operario");
        saved.setDepartamento(dept);
        saved.setActivo(true);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(puestoRepository.save(any(Puesto.class))).thenReturn(saved);

        // When categoriaSalarialMinima is null, NO SalarioMinimoService call must happen
        Puesto result = service.crear(dto);

        assertNotNull(result);
        assertEquals("Operario", result.getNombre());
        verify(puestoRepository).save(any(Puesto.class));
    }

    @Test
    @DisplayName("crear() with non-null categoriaSalarialMinima must enforce min-wage check (PR2 wired)")
    void crear_nonNullCategoriaSalarial_enforceMinWageAndPersists() {
        Long deptId = 2L;
        PuestoDTO dto = new PuestoDTO();
        dto.setNombre("Cajero");
        dto.setDepartamentoId(deptId);
        dto.setSalarioBase(BigDecimal.valueOf(380000)); // above TONC minimum (373092.30)
        dto.setCategoriaSalarialMinima("TONC");
        dto.setTipoJornada(TipoJornada.DIURNA);

        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setNombre("Ventas");
        dept.setActivo(true);

        // PR2: SalarioMinimoService is now wired — must stub it
        SalarioMinimo salarioMinimo = new SalarioMinimo();
        salarioMinimo.setCategoria("TONC");
        salarioMinimo.setMontoMensual(new BigDecimal("373092.30"));
        salarioMinimo.setVigenciaDesde(LocalDate.of(2026, 1, 1));
        salarioMinimo.setCreatedAt(LocalDateTime.now());

        Puesto saved = new Puesto();
        saved.setId(11L);
        saved.setNombre("Cajero");
        saved.setCategoriaSalarialMinima("TONC");
        saved.setDepartamento(dept);
        saved.setActivo(true);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(salarioMinimoService.findVigenteByCategoria(eq("TONC"), any(LocalDate.class)))
                .thenReturn(salarioMinimo);
        when(puestoRepository.save(any(Puesto.class))).thenReturn(saved);

        // salarioBase (380000) >= minimo (373092.30) → should persist
        Puesto result = service.crear(dto);

        assertNotNull(result);
        assertEquals("Cajero", result.getNombre());
        assertEquals("TONC", result.getCategoriaSalarialMinima());
        verify(salarioMinimoService).findVigenteByCategoria(eq("TONC"), any(LocalDate.class));
        verify(puestoRepository).save(any(Puesto.class));
    }

    // =========================================================================
    // desactivar()
    // =========================================================================

    @Test
    @DisplayName("desactivar() must set activo=false and save")
    void desactivar_setsActivoFalse() {
        Long id = 5L;
        Puesto puesto = new Puesto();
        puesto.setId(id);
        puesto.setNombre("Contador");
        puesto.setActivo(true);

        when(puestoRepository.findById(id)).thenReturn(Optional.of(puesto));
        when(puestoRepository.save(puesto)).thenReturn(puesto);

        service.desactivar(id);

        assertFalse(puesto.isActivo());
        verify(puestoRepository).save(puesto);
    }

    @Test
    @DisplayName("desactivar() must throw when puesto not found")
    void desactivar_notFound_throwsException() {
        when(puestoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.desactivar(99L));
    }

    // =========================================================================
    // findByDepartamento()
    // =========================================================================

    @Test
    @DisplayName("findByDepartamento() must return active puestos for the given department")
    void findByDepartamento_returnsActivePuestos() {
        Long deptId = 1L;
        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setActivo(true);

        Puesto p = new Puesto();
        p.setId(1L);
        p.setNombre("Analista");
        p.setActivo(true);
        p.setDepartamento(dept);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(puestoRepository.findByDepartamentoIdAndActivoTrue(deptId)).thenReturn(List.of(p));

        List<Puesto> result = service.findByDepartamento(deptId);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActivo());
    }

    // =========================================================================
    // PR2: min-wage enforcement via SalarioMinimoService
    // =========================================================================

    @Test
    @DisplayName("crear() must throw IllegalArgumentException when salarioBase < salario minimo vigente")
    void crear_salarioBaseBelowMinimum_throwsIllegalArgument() {
        // GIVEN category "TONC" has vigente monto=373092.30 for 2026-01-01
        Long deptId = 3L;
        PuestoDTO dto = new PuestoDTO();
        dto.setNombre("Auxiliar");
        dto.setDepartamentoId(deptId);
        dto.setSalarioBase(new BigDecimal("300000.00")); // below minimum 373092.30
        dto.setCategoriaSalarialMinima("TONC");
        dto.setTipoJornada(TipoJornada.DIURNA);

        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setNombre("Operaciones");
        dept.setActivo(true);

        SalarioMinimo salarioMinimo = new SalarioMinimo();
        salarioMinimo.setCategoria("TONC");
        salarioMinimo.setMontoMensual(new BigDecimal("373092.30"));
        salarioMinimo.setVigenciaDesde(LocalDate.of(2026, 1, 1));
        salarioMinimo.setVigenciaHasta(LocalDate.of(2026, 6, 30));
        salarioMinimo.setCreatedAt(LocalDateTime.now());

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(salarioMinimoService.findVigenteByCategoria(eq("TONC"), any(LocalDate.class)))
                .thenReturn(salarioMinimo);

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crear(dto));

        String msg = ex.getMessage().toLowerCase();
        assertTrue(
            msg.contains("salario") || msg.contains("minimo") || msg.contains("mínimo"),
            "Exception must mention salary minimum violation. Got: " + ex.getMessage()
        );
        verify(puestoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() must succeed when salarioBase equals salario minimo vigente (boundary)")
    void crear_salarioBaseEqualsMinimum_persists() {
        Long deptId = 3L;
        PuestoDTO dto = new PuestoDTO();
        dto.setNombre("Auxiliar");
        dto.setDepartamentoId(deptId);
        dto.setSalarioBase(new BigDecimal("373092.30")); // exactly at minimum
        dto.setCategoriaSalarialMinima("TONC");
        dto.setTipoJornada(TipoJornada.DIURNA);

        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setNombre("Operaciones");
        dept.setActivo(true);

        SalarioMinimo salarioMinimo = new SalarioMinimo();
        salarioMinimo.setCategoria("TONC");
        salarioMinimo.setMontoMensual(new BigDecimal("373092.30"));
        salarioMinimo.setVigenciaDesde(LocalDate.of(2026, 1, 1));
        salarioMinimo.setCreatedAt(LocalDateTime.now());

        Puesto saved = new Puesto();
        saved.setId(12L);
        saved.setNombre("Auxiliar");
        saved.setSalarioBase(new BigDecimal("373092.30"));
        saved.setCategoriaSalarialMinima("TONC");
        saved.setDepartamento(dept);
        saved.setActivo(true);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(salarioMinimoService.findVigenteByCategoria(eq("TONC"), any(LocalDate.class)))
                .thenReturn(salarioMinimo);
        when(puestoRepository.save(any(Puesto.class))).thenReturn(saved);

        Puesto result = service.crear(dto);

        assertNotNull(result);
        assertEquals("Auxiliar", result.getNombre());
        verify(puestoRepository).save(any(Puesto.class));
    }

    @Test
    @DisplayName("crear() must skip min-wage check when categoriaSalarialMinima is null (PR2 wire-back)")
    void crear_nullCategoria_skipsMinWageCheckAndPersists_afterPR2Wire() {
        Long deptId = 4L;
        PuestoDTO dto = new PuestoDTO();
        dto.setNombre("Consultor");
        dto.setDepartamentoId(deptId);
        dto.setSalarioBase(new BigDecimal("100000.00")); // any amount — check is skipped
        dto.setCategoriaSalarialMinima(null);
        dto.setTipoJornada(TipoJornada.DIURNA);

        Departamento dept = new Departamento();
        dept.setId(deptId);
        dept.setNombre("Administración");
        dept.setActivo(true);

        Puesto saved = new Puesto();
        saved.setId(20L);
        saved.setNombre("Consultor");
        saved.setDepartamento(dept);
        saved.setActivo(true);

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(puestoRepository.save(any(Puesto.class))).thenReturn(saved);

        Puesto result = service.crear(dto);

        assertNotNull(result);
        // SalarioMinimoService must NOT be called when categoria is null
        verify(salarioMinimoService, never()).findVigenteByCategoria(any(), any());
        verify(puestoRepository).save(any(Puesto.class));
    }
}
