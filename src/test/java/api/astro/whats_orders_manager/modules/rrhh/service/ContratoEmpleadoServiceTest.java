package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.ContratoEmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.CausaTerminacion;
import api.astro.whats_orders_manager.modules.rrhh.enums.FormaPago;
import api.astro.whats_orders_manager.modules.rrhh.enums.PeriodicidadPago;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoContrato;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import api.astro.whats_orders_manager.modules.rrhh.model.ContratoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.ContratoEmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.ContratoEmpleadoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContratoEmpleadoServiceImpl.
 *
 * RED phase — written before implementation exists.
 *
 * Covers:
 * PR4.5 — crear() deactivates previous active contract before persisting new one
 * PR4.6 — crear() works when employee has no prior contracts
 * PR4.7 — terminar() sets activo=false, causa, fechaTerminacion, descripcion
 * PR4.8 — findByEmpleado() delegates to repository
 * PR4.9 — crear() throws when empleado not found
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContratoEmpleadoServiceImpl — unit tests")
class ContratoEmpleadoServiceTest {

    @Mock
    private ContratoEmpleadoRepository contratoRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    private ContratoEmpleadoService service;

    private Empleado empleado;

    @BeforeEach
    void setUp() {
        service = new ContratoEmpleadoServiceImpl(contratoRepository, empleadoRepository);

        empleado = new Empleado();
        empleado.setId(5L);
        empleado.setNombre("Ana");
        empleado.setPrimerApellido("González");
        empleado.setActivo(true);
    }

    // =========================================================================
    // PR4.5 — crear() deactivates previous active contract
    // =========================================================================

    @Test
    @DisplayName("crear() must deactivate the existing active contract before persisting the new one")
    void crear_withExistingActiveContract_deactivatesPrevious() {
        // GIVEN employee 5 has active contract C1
        ContratoEmpleado existing = buildContrato(empleado, TipoContrato.INDEFINIDO, true);
        existing.setId(10L);

        ContratoEmpleadoDTO dto = buildDTO(5L);

        ContratoEmpleado saved = buildContrato(empleado, TipoContrato.PLAZO_FIJO, true);
        saved.setId(11L);

        when(empleadoRepository.findById(5L)).thenReturn(Optional.of(empleado));
        when(contratoRepository.findByEmpleadoIdAndActivoTrue(5L))
                .thenReturn(Optional.of(existing));
        when(contratoRepository.save(any(ContratoEmpleado.class))).thenReturn(saved);

        // WHEN
        service.crear(dto);

        // THEN — previous contract must be deactivated
        assertFalse(existing.getActivo(),
                "Previous contract must have activo=false after creating new contract");

        // AND save must be called at least twice (deactivate old, persist new)
        verify(contratoRepository, atLeast(2)).save(any(ContratoEmpleado.class));
    }

    // =========================================================================
    // PR4.6 — crear() works when employee has no prior contracts
    // =========================================================================

    @Test
    @DisplayName("crear() must succeed when employee has no previous contracts")
    void crear_noPreviousContract_persistsSuccessfully() {
        ContratoEmpleadoDTO dto = buildDTO(5L);

        ContratoEmpleado saved = buildContrato(empleado, TipoContrato.INDEFINIDO, true);
        saved.setId(11L);

        when(empleadoRepository.findById(5L)).thenReturn(Optional.of(empleado));
        when(contratoRepository.findByEmpleadoIdAndActivoTrue(5L))
                .thenReturn(Optional.empty());
        when(contratoRepository.save(any(ContratoEmpleado.class))).thenReturn(saved);

        // WHEN
        ContratoEmpleado result = service.crear(dto);

        // THEN
        assertNotNull(result, "Created contract must not be null");
        assertTrue(result.getActivo(), "New contract must be activo=true");
        verify(contratoRepository).save(any(ContratoEmpleado.class));
    }

    @Test
    @DisplayName("crear() must deactivate previous contract even when employee id differs (second employee)")
    void crear_segundoEmpleadoConContratoActivo_deactivatesPrevious() {
        // GIVEN a different employee (id=12)
        Empleado empleado2 = new Empleado();
        empleado2.setId(12L);
        empleado2.setNombre("Mario");
        empleado2.setPrimerApellido("Torres");
        empleado2.setActivo(true);

        ContratoEmpleado existingContrato = buildContrato(empleado2, TipoContrato.PLAZO_FIJO, true);
        existingContrato.setId(20L);

        ContratoEmpleadoDTO dto2 = new ContratoEmpleadoDTO();
        dto2.setEmpleadoId(12L);
        dto2.setTipoContrato(TipoContrato.INDEFINIDO);
        dto2.setFechaInicio(LocalDate.of(2026, 3, 1));
        dto2.setSalarioBruto(new BigDecimal("700000.00"));
        dto2.setJornada(TipoJornada.DIURNA);
        dto2.setCargoContratado("Gerente de Proyectos");

        ContratoEmpleado savedNew = buildContrato(empleado2, TipoContrato.INDEFINIDO, true);
        savedNew.setId(21L);

        when(empleadoRepository.findById(12L)).thenReturn(Optional.of(empleado2));
        when(contratoRepository.findByEmpleadoIdAndActivoTrue(12L))
                .thenReturn(Optional.of(existingContrato));
        when(contratoRepository.save(any(ContratoEmpleado.class))).thenReturn(savedNew);

        service.crear(dto2);

        assertFalse(existingContrato.getActivo(),
                "Previous PLAZO_FIJO contract must be deactivated");
        verify(contratoRepository, atLeast(2)).save(any(ContratoEmpleado.class));
    }

    // =========================================================================
    // PR4.7 — terminar() sets all required fields
    // =========================================================================

    @Test
    @DisplayName("terminar() must set activo=false, causa, fechaTerminacion and descripcion")
    void terminar_setsAllTerminationFields() {
        // GIVEN
        Long contratoId = 10L;
        ContratoEmpleado contrato = buildContrato(empleado, TipoContrato.INDEFINIDO, true);
        contrato.setId(contratoId);

        LocalDate fecha = LocalDate.of(2026, 7, 21);
        String descripcion = "Renuncia presentada por escrito";

        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(contrato)).thenReturn(contrato);

        // WHEN
        service.terminar(contratoId, CausaTerminacion.RENUNCIA_VOLUNTARIA, fecha, descripcion);

        // THEN
        assertFalse(contrato.getActivo(), "activo must be false after terminar()");
        assertEquals(CausaTerminacion.RENUNCIA_VOLUNTARIA, contrato.getCausaTerminacion(),
                "causaTerminacion must be set");
        assertEquals(fecha, contrato.getFechaTerminacion(),
                "fechaTerminacion must match the provided date");
        assertEquals(descripcion, contrato.getDescripcionTerminacion(),
                "descripcionTerminacion must match");
        verify(contratoRepository).save(contrato);
    }

    @Test
    @DisplayName("terminar() must correctly record DESPIDO causa with a different date and description")
    void terminar_causaDespido_setsCorrectFields() {
        Long contratoId = 30L;
        ContratoEmpleado contrato = buildContrato(empleado, TipoContrato.PLAZO_FIJO, true);
        contrato.setId(contratoId);

        LocalDate fecha = LocalDate.of(2026, 2, 28);
        String descripcion = "Falta grave según reglamento interno";

        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(contrato)).thenReturn(contrato);

        service.terminar(contratoId, CausaTerminacion.DESPIDO_CON_CAUSA, fecha, descripcion);

        assertFalse(contrato.getActivo());
        assertEquals(CausaTerminacion.DESPIDO_CON_CAUSA, contrato.getCausaTerminacion());
        assertEquals(fecha, contrato.getFechaTerminacion());
        assertEquals(descripcion, contrato.getDescripcionTerminacion());
        verify(contratoRepository).save(contrato);
    }

    // =========================================================================
    // PR4.8 — findByEmpleado() delegates to repository
    // =========================================================================

    @Test
    @DisplayName("findByEmpleado() must return contracts for the given empleado id")
    void findByEmpleado_delegatesToRepository() {
        ContratoEmpleado c1 = buildContrato(empleado, TipoContrato.INDEFINIDO, false);
        ContratoEmpleado c2 = buildContrato(empleado, TipoContrato.PLAZO_FIJO, true);

        when(contratoRepository.findByEmpleadoId(5L)).thenReturn(List.of(c1, c2));

        List<ContratoEmpleado> result = service.findByEmpleado(5L);

        assertEquals(2, result.size());
        verify(contratoRepository).findByEmpleadoId(5L);
    }

    @Test
    @DisplayName("findByEmpleado() must return contracts for a second different empleado id")
    void findByEmpleado_otroEmpleado_delegatesToRepository() {
        Empleado otro = new Empleado();
        otro.setId(99L);
        otro.setNombre("Pedro");
        otro.setPrimerApellido("Soto");
        otro.setActivo(true);

        ContratoEmpleado c = buildContrato(otro, TipoContrato.INDEFINIDO, true);

        when(contratoRepository.findByEmpleadoId(99L)).thenReturn(List.of(c));

        List<ContratoEmpleado> result = service.findByEmpleado(99L);

        assertEquals(1, result.size());
        verify(contratoRepository).findByEmpleadoId(99L);
    }

    // =========================================================================
    // PR4.9 — crear() throws when empleado not found
    // =========================================================================

    @Test
    @DisplayName("crear() must throw NoSuchElementException when empleado not found")
    void crear_empleadoNotFound_throws() {
        ContratoEmpleadoDTO dto = buildDTO(99L);
        when(empleadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.crear(dto));
        verify(contratoRepository, never()).save(any());
    }

    // =========================================================================
    // W3 — crear() validates justificacionTemporalidad for temporal contracts
    // =========================================================================

    @Test
    @DisplayName("crear() must throw IllegalArgumentException for PLAZO_FIJO without justificacion")
    void crear_plazoFijoSinJustificacion_throwsIllegalArgument() {
        ContratoEmpleadoDTO dto = buildDTO(5L);
        dto.setTipoContrato(TipoContrato.PLAZO_FIJO);
        dto.setJustificacionTemporalidad(null);

        when(empleadoRepository.findById(5L)).thenReturn(Optional.of(empleado));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(dto));

        assertTrue(ex.getMessage().toLowerCase().contains("justificacion"),
                "Error must mention justificacion. Got: " + ex.getMessage());
        verify(contratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear() must throw IllegalArgumentException for OBRA_DETERMINADA with blank justificacion")
    void crear_obraDeterminadaSinJustificacion_throwsIllegalArgument() {
        ContratoEmpleadoDTO dto = buildDTO(5L);
        dto.setTipoContrato(TipoContrato.OBRA_DETERMINADA);
        dto.setJustificacionTemporalidad("   ");

        when(empleadoRepository.findById(5L)).thenReturn(Optional.of(empleado));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(dto));

        assertTrue(ex.getMessage().toLowerCase().contains("justificacion"),
                "Error must mention justificacion. Got: " + ex.getMessage());
        verify(contratoRepository, never()).save(any());
    }

    // =========================================================================
    // G1.1 — crear() with TIEMPO_PARCIAL persists tipo correctly
    // =========================================================================

    @Test
    @DisplayName("crear() with TIEMPO_PARCIAL contract type must persist tipo correctly")
    void crear_tiempoParcial_persists() {
        // GIVEN
        ContratoEmpleadoDTO dto = buildDTO(5L);
        dto.setTipoContrato(TipoContrato.TIEMPO_PARCIAL);

        ContratoEmpleado saved = buildContrato(empleado, TipoContrato.TIEMPO_PARCIAL, true);
        saved.setId(50L);

        when(empleadoRepository.findById(5L)).thenReturn(Optional.of(empleado));
        when(contratoRepository.findByEmpleadoIdAndActivoTrue(5L)).thenReturn(Optional.empty());
        when(contratoRepository.save(any(ContratoEmpleado.class))).thenReturn(saved);

        // WHEN
        ContratoEmpleado result = service.crear(dto);

        // THEN
        assertNotNull(result);
        assertEquals(TipoContrato.TIEMPO_PARCIAL, result.getTipoContrato(),
                "tipoContrato must be TIEMPO_PARCIAL");
        verify(contratoRepository).save(any(ContratoEmpleado.class));
    }

    // =========================================================================
    // G1.2 — crear() with lugarTrabajo persists field correctly
    // =========================================================================

    @Test
    @DisplayName("crear() with lugarTrabajo must persist the field value")
    void crear_conLugarTrabajo_persists() {
        // GIVEN
        ContratoEmpleadoDTO dto = buildDTO(5L);
        dto.setLugarTrabajo("Sede Central");

        ContratoEmpleado saved = buildContrato(empleado, TipoContrato.INDEFINIDO, true);
        saved.setId(51L);
        saved.setLugarTrabajo("Sede Central");

        when(empleadoRepository.findById(5L)).thenReturn(Optional.of(empleado));
        when(contratoRepository.findByEmpleadoIdAndActivoTrue(5L)).thenReturn(Optional.empty());
        when(contratoRepository.save(any(ContratoEmpleado.class))).thenReturn(saved);

        // WHEN
        ContratoEmpleado result = service.crear(dto);

        // THEN
        assertNotNull(result);
        assertEquals("Sede Central", result.getLugarTrabajo(),
                "lugarTrabajo must be 'Sede Central'");
        verify(contratoRepository).save(any(ContratoEmpleado.class));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ContratoEmpleadoDTO buildDTO(Long empleadoId) {
        ContratoEmpleadoDTO dto = new ContratoEmpleadoDTO();
        dto.setEmpleadoId(empleadoId);
        dto.setTipoContrato(TipoContrato.INDEFINIDO);
        dto.setFechaInicio(LocalDate.of(2026, 1, 1));
        dto.setSalarioBruto(new BigDecimal("600000.00"));
        dto.setJornada(TipoJornada.DIURNA);
        dto.setCargoContratado("Desarrollador Backend");
        return dto;
    }

    private ContratoEmpleado buildContrato(Empleado emp, TipoContrato tipo, boolean activo) {
        ContratoEmpleado c = new ContratoEmpleado();
        c.setEmpleado(emp);
        c.setTipoContrato(tipo);
        c.setFechaInicio(LocalDate.of(2025, 6, 1));
        c.setSalarioBruto(new BigDecimal("500000.00"));
        c.setActivo(activo);
        c.setJornada(TipoJornada.DIURNA);
        return c;
    }
}
