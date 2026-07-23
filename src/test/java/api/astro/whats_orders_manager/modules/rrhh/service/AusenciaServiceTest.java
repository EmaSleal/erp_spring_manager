package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.AusenciaDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoAusencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.AusenciaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.AusenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AusenciaServiceImpl.
 *
 * RED phase — written before implementation exists.
 *
 * Covers:
 * PR4.10 — registrar() rejects overlap with approved ausencia
 * PR4.11 — registrar() succeeds when range does not overlap approved ausencia
 * PR4.12 — registrar() succeeds when overlap is only with unapproved ausencia
 * PR4.13 — aprobar() sets aprobada=true and aprobadaPor
 * PR4.14 — findByEmpleado() delegates to repository
 * PR4.15 — registrar() throws when empleado not found
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AusenciaServiceImpl — unit tests")
class AusenciaServiceTest {

    @Mock
    private AusenciaRepository ausenciaRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private AusenciaService service;

    private Empleado empleado;
    private Usuario aprobador;

    @BeforeEach
    void setUp() {
        service = new AusenciaServiceImpl(ausenciaRepository, empleadoRepository, usuarioRepository);

        empleado = new Empleado();
        empleado.setId(3L);
        empleado.setNombre("Carlos");
        empleado.setPrimerApellido("López");
        empleado.setActivo(true);

        aprobador = new Usuario();
        aprobador.setIdUsuario(1);
    }

    // =========================================================================
    // PR4.10 — registrar() rejects overlap with approved ausencia
    // =========================================================================

    @Test
    @DisplayName("registrar() must reject range overlapping an approved ausencia")
    void registrar_overlapsApprovedAusencia_throwsIllegalArgument() {
        // GIVEN employee 3 has approved ausencia 2026-07-01 to 2026-07-10
        Ausencia approved = buildAusencia(empleado,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 10),
                true);

        // New request overlaps: 2026-07-08 to 2026-07-15
        AusenciaDTO dto = buildDTO(3L,
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 15),
                null);

        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of(approved));

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(dto));

        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("overlap") || msg.contains("traslapo") || msg.contains("ausencia"),
                "Error must mention overlap. Got: " + ex.getMessage());
        verify(ausenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar() must also reject a second distinct overlapping range with approved ausencia")
    void registrar_segundoTraslapeAprobada_throwsIllegalArgument() {
        // GIVEN employee 3 has approved ausencia 2026-08-01 to 2026-08-20
        Ausencia approved = buildAusencia(empleado,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 20),
                true);

        // New request overlaps: 2026-08-15 to 2026-08-25
        AusenciaDTO dto = buildDTO(3L,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 25),
                null);

        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of(approved));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(dto));
        verify(ausenciaRepository, never()).save(any());
    }

    // =========================================================================
    // PR4.11 — registrar() succeeds when range does not overlap
    // =========================================================================

    @Test
    @DisplayName("registrar() must succeed when new range does not overlap approved ausencias")
    void registrar_noOverlap_persistsSuccessfully() {
        // GIVEN employee 3 has approved ausencia 2026-07-01 to 2026-07-10
        Ausencia approved = buildAusencia(empleado,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 10),
                true);

        // New request: 2026-07-11 to 2026-07-15 (no overlap)
        AusenciaDTO dto = buildDTO(3L,
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 15),
                null);

        Ausencia saved = buildAusencia(empleado,
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 15),
                false);
        saved.setId(20L);

        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of(approved));
        when(ausenciaRepository.save(any(Ausencia.class))).thenReturn(saved);

        // WHEN
        Ausencia result = service.registrar(dto);

        // THEN
        assertNotNull(result);
        assertFalse(result.getAprobada(), "New ausencia must not be approved initially");
        verify(ausenciaRepository).save(any(Ausencia.class));
    }

    @Test
    @DisplayName("registrar() must succeed for a second distinct non-overlapping range")
    void registrar_segundoRangoSinTraslape_persistsSuccessfully() {
        // GIVEN approved ausencia 2026-09-01 to 2026-09-10
        Ausencia approved = buildAusencia(empleado,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 10),
                true);

        // New request completely before: 2026-08-15 to 2026-08-31
        AusenciaDTO dto = buildDTO(3L,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 31),
                null);

        Ausencia saved = buildAusencia(empleado,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 31),
                false);
        saved.setId(22L);

        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of(approved));
        when(ausenciaRepository.save(any(Ausencia.class))).thenReturn(saved);

        Ausencia result = service.registrar(dto);

        assertNotNull(result);
        assertFalse(result.getAprobada());
        verify(ausenciaRepository).save(any(Ausencia.class));
    }

    // =========================================================================
    // PR4.12 — registrar() succeeds when overlap is only with unapproved ausencia
    // =========================================================================

    @Test
    @DisplayName("registrar() must succeed when overlap is only with unapproved ausencias")
    void registrar_overlapsOnlyUnapproved_persistsSuccessfully() {
        // GIVEN employee 3 has unapproved ausencia 2026-07-01 to 2026-07-10
        Ausencia unapproved = buildAusencia(empleado,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 10),
                false);

        // New request overlaps 2026-07-05 to 2026-07-12
        AusenciaDTO dto = buildDTO(3L,
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 12),
                null);

        Ausencia saved = buildAusencia(empleado,
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 12),
                false);
        saved.setId(21L);

        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of(unapproved));
        when(ausenciaRepository.save(any(Ausencia.class))).thenReturn(saved);

        // WHEN — must NOT throw
        Ausencia result = service.registrar(dto);

        assertNotNull(result);
        verify(ausenciaRepository).save(any(Ausencia.class));
    }

    // =========================================================================
    // PR4.13 — aprobar() sets aprobada=true and aprobadaPor
    // =========================================================================

    @Test
    @DisplayName("aprobar() must set aprobada=true and aprobadaPor to the given usuario")
    void aprobar_setsApprovalFields() {
        // GIVEN ausencia with aprobada=false
        Long ausenciaId = 20L;
        Ausencia ausencia = buildAusencia(empleado,
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 15),
                false);
        ausencia.setId(ausenciaId);

        when(ausenciaRepository.findById(ausenciaId)).thenReturn(Optional.of(ausencia));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(aprobador));
        when(ausenciaRepository.save(ausencia)).thenReturn(ausencia);

        // WHEN
        service.aprobar(ausenciaId, 1);

        // THEN
        assertTrue(ausencia.getAprobada(), "aprobada must be true after aprobar()");
        assertEquals(aprobador, ausencia.getAprobadaPor(),
                "aprobadaPor must be set to the provided usuario");
        assertNotNull(ausencia.getFechaAprobacion(),
                "fechaAprobacion must be set after aprobar()");
        verify(ausenciaRepository).save(ausencia);
    }

    @Test
    @DisplayName("aprobar() must record fechaAprobacion for a second distinct ausencia")
    void aprobar_segundaAusencia_setsFechaAprobacion() {
        Long ausenciaId = 42L;
        Ausencia ausencia = buildAusencia(empleado,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                false);
        ausencia.setId(ausenciaId);

        when(ausenciaRepository.findById(ausenciaId)).thenReturn(Optional.of(ausencia));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(aprobador));
        when(ausenciaRepository.save(ausencia)).thenReturn(ausencia);

        LocalDateTime before = LocalDateTime.now();
        service.aprobar(ausenciaId, 1);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(ausencia.getFechaAprobacion(),
                "fechaAprobacion must not be null after aprobar()");
        assertFalse(ausencia.getFechaAprobacion().isBefore(before),
                "fechaAprobacion must be >= timestamp before call");
        assertFalse(ausencia.getFechaAprobacion().isAfter(after),
                "fechaAprobacion must be <= timestamp after call");
    }

    @Test
    @DisplayName("aprobar() must set aprobada=true and aprobadaPor for a second distinct ausencia and aprobador")
    void aprobar_segundaAusenciaDistintoAprobador_setsApprovalFields() {
        Long ausenciaId = 35L;
        Ausencia ausencia = buildAusencia(empleado,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5),
                false);
        ausencia.setId(ausenciaId);

        Usuario aprobador2 = new Usuario();
        aprobador2.setIdUsuario(7);

        when(ausenciaRepository.findById(ausenciaId)).thenReturn(Optional.of(ausencia));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(aprobador2));
        when(ausenciaRepository.save(ausencia)).thenReturn(ausencia);

        service.aprobar(ausenciaId, 7);

        assertTrue(ausencia.getAprobada());
        assertEquals(aprobador2, ausencia.getAprobadaPor());
        assertNotNull(ausencia.getFechaAprobacion(),
                "fechaAprobacion must be set after aprobar() for second aprobador");
        verify(ausenciaRepository).save(ausencia);
    }

    // =========================================================================
    // PR4.14 — findByEmpleado() delegates to repository
    // =========================================================================

    @Test
    @DisplayName("findByEmpleado() must return ausencias for the given empleado id")
    void findByEmpleado_delegatesToRepository() {
        Ausencia a1 = buildAusencia(empleado, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15), false);
        Ausencia a2 = buildAusencia(empleado, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), true);

        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of(a1, a2));

        List<Ausencia> result = service.findByEmpleado(3L);

        assertEquals(2, result.size());
        verify(ausenciaRepository).findByEmpleadoId(3L);
    }

    @Test
    @DisplayName("findByEmpleado() must return ausencias for a second distinct empleado id")
    void findByEmpleado_otroEmpleado_delegatesToRepository() {
        Empleado empleado2 = new Empleado();
        empleado2.setId(8L);
        empleado2.setNombre("Lucía");
        empleado2.setPrimerApellido("Vega");
        empleado2.setActivo(true);

        Ausencia a = buildAusencia(empleado2, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), true);

        when(ausenciaRepository.findByEmpleadoId(8L)).thenReturn(List.of(a));

        List<Ausencia> result = service.findByEmpleado(8L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getAprobada());
        verify(ausenciaRepository).findByEmpleadoId(8L);
    }

    // =========================================================================
    // PR4.15 — registrar() throws when empleado not found
    // =========================================================================

    @Test
    @DisplayName("registrar() must throw NoSuchElementException when empleado not found")
    void registrar_empleadoNotFound_throws() {
        AusenciaDTO dto = buildDTO(99L,
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 15),
                null);

        when(empleadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.registrar(dto));
        verify(ausenciaRepository, never()).save(any());
    }

    // =========================================================================
    // G1.3 — registrar() with INCAPACIDAD_INS persists tipo correctly
    // =========================================================================

    @Test
    @DisplayName("registrar() with INCAPACIDAD_INS must persist tipoAusencia correctly")
    void registrar_incapacidadINS_persists() {
        // GIVEN
        AusenciaDTO dto = buildDTO(3L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 5),
                null);
        dto.setTipoAusencia(TipoAusencia.INCAPACIDAD_INS);

        Ausencia saved = buildAusencia(empleado,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 5),
                false);
        saved.setId(30L);
        saved.setTipoAusencia(TipoAusencia.INCAPACIDAD_INS);

        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of());
        when(ausenciaRepository.save(any(Ausencia.class))).thenReturn(saved);

        // WHEN
        Ausencia result = service.registrar(dto);

        // THEN
        assertNotNull(result);
        assertEquals(TipoAusencia.INCAPACIDAD_INS, result.getTipoAusencia(),
                "tipoAusencia must be INCAPACIDAD_INS");
        verify(ausenciaRepository).save(any(Ausencia.class));
    }

    // =========================================================================
    // G1.4 — registrar() with entidadCertificante and numeroBoleta saves both fields
    // =========================================================================

    @Test
    @DisplayName("registrar() must persist entidadCertificante and numeroBoleta when provided")
    void registrar_conCertificacion_persists() {
        // GIVEN
        AusenciaDTO dto = buildDTO(3L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 5),
                null);
        dto.setTipoAusencia(TipoAusencia.INCAPACIDAD_INS);
        dto.setEntidadCertificante("INS");
        dto.setNumeroBoleta("12345");

        Ausencia saved = buildAusencia(empleado,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 5),
                false);
        saved.setId(31L);
        saved.setTipoAusencia(TipoAusencia.INCAPACIDAD_INS);
        saved.setEntidadCertificante("INS");
        saved.setNumeroBoleta("12345");

        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(ausenciaRepository.findByEmpleadoId(3L)).thenReturn(List.of());
        when(ausenciaRepository.save(any(Ausencia.class))).thenReturn(saved);

        // WHEN
        Ausencia result = service.registrar(dto);

        // THEN
        assertNotNull(result);
        assertEquals("INS", result.getEntidadCertificante(),
                "entidadCertificante must be 'INS'");
        assertEquals("12345", result.getNumeroBoleta(),
                "numeroBoleta must be '12345'");
        verify(ausenciaRepository).save(any(Ausencia.class));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AusenciaDTO buildDTO(Long empleadoId, LocalDate inicio, LocalDate fin, Integer aprobadorId) {
        AusenciaDTO dto = new AusenciaDTO();
        dto.setEmpleadoId(empleadoId);
        dto.setTipoAusencia(TipoAusencia.VACACIONES);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setConGoceSalario(true);
        dto.setComputaParaAguinaldo(true);
        dto.setComputaAntiguedad(true);
        dto.setJustificada(true);
        dto.setAprobadaPorId(aprobadorId);
        return dto;
    }

    private Ausencia buildAusencia(Empleado emp, LocalDate inicio, LocalDate fin, boolean aprobada) {
        Ausencia a = new Ausencia();
        a.setEmpleado(emp);
        a.setTipoAusencia(TipoAusencia.VACACIONES);
        a.setFechaInicio(inicio);
        a.setFechaFin(fin);
        a.setAprobada(aprobada);
        a.setConGoceSalario(true);
        a.setComputaParaAguinaldo(true);
        a.setComputaAntiguedad(true);
        a.setJustificada(true);
        return a;
    }
}
