package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.model.SaldoVacaciones;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.SaldoVacacionesRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.SaldoVacacionesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SaldoVacacionesServiceImpl.
 *
 * RED phase — written before implementation exists.
 *
 * Covers:
 * G2.1 — inicializar() creates record with 0 dias
 * G2.2 — acreditarDias() first acreditation sums days
 * G2.3 — acreditarDias() second acreditation accumulates
 * G2.4 — descontarDias() with sufficient balance subtracts
 * G2.5 — descontarDias() with insufficient balance throws IllegalArgumentException
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SaldoVacacionesServiceImpl — unit tests")
class SaldoVacacionesServiceTest {

    @Mock
    private SaldoVacacionesRepository saldoVacacionesRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    private SaldoVacacionesService service;

    private Empleado empleado;

    @BeforeEach
    void setUp() {
        service = new SaldoVacacionesServiceImpl(saldoVacacionesRepository, empleadoRepository);

        empleado = new Empleado();
        empleado.setId(10L);
        empleado.setNombre("María");
        empleado.setPrimerApellido("Pérez");
        empleado.setActivo(true);
    }

    // =========================================================================
    // G2.1 — inicializar() creates record with 0 dias
    // =========================================================================

    @Test
    @DisplayName("inicializar() must create a SaldoVacaciones record with diasGenerados=0 and diasDisfrutados=0")
    void inicializar_creaRegistroCon0Dias_ok() {
        // GIVEN employee 10 has no saldo yet
        when(empleadoRepository.findById(10L)).thenReturn(Optional.of(empleado));

        ArgumentCaptor<SaldoVacaciones> captor = ArgumentCaptor.forClass(SaldoVacaciones.class);
        SaldoVacaciones saved = new SaldoVacaciones();
        saved.setId(1L);
        saved.setEmpleado(empleado);
        saved.setDiasGenerados(BigDecimal.ZERO);
        saved.setDiasDisfrutados(BigDecimal.ZERO);
        saved.setFechaUltimoCalculo(LocalDate.now());
        when(saldoVacacionesRepository.save(captor.capture())).thenReturn(saved);

        // WHEN
        service.inicializar(10L);

        // THEN
        SaldoVacaciones persisted = captor.getValue();
        assertEquals(0, BigDecimal.ZERO.compareTo(persisted.getDiasGenerados()),
                "diasGenerados must be 0 on initialization");
        assertEquals(0, BigDecimal.ZERO.compareTo(persisted.getDiasDisfrutados()),
                "diasDisfrutados must be 0 on initialization");
        assertNotNull(persisted.getFechaUltimoCalculo(),
                "fechaUltimoCalculo must be set on initialization");
        assertEquals(LocalDate.now(), persisted.getFechaUltimoCalculo(),
                "fechaUltimoCalculo must be today");
        verify(saldoVacacionesRepository).save(any(SaldoVacaciones.class));
    }

    // =========================================================================
    // G2.2 — acreditarDias() first acreditation sums days
    // =========================================================================

    @Test
    @DisplayName("acreditarDias() must add dias to diasGenerados on first acreditation")
    void acreditar_primerAcreditacion_sumaDias() {
        // GIVEN no existing saldo
        when(empleadoRepository.findById(10L)).thenReturn(Optional.of(empleado));
        when(saldoVacacionesRepository.findByEmpleadoId(10L)).thenReturn(Optional.empty());

        ArgumentCaptor<SaldoVacaciones> captor = ArgumentCaptor.forClass(SaldoVacaciones.class);
        SaldoVacaciones saved = new SaldoVacaciones();
        saved.setEmpleado(empleado);
        saved.setDiasGenerados(new BigDecimal("10.00"));
        saved.setDiasDisfrutados(BigDecimal.ZERO);
        saved.setFechaUltimoCalculo(LocalDate.now());
        when(saldoVacacionesRepository.save(captor.capture())).thenReturn(saved);

        // WHEN
        service.acreditarDias(10L, new BigDecimal("10.00"));

        // THEN
        SaldoVacaciones persisted = captor.getValue();
        assertEquals(0, new BigDecimal("10.00").compareTo(persisted.getDiasGenerados()),
                "diasGenerados must be 10.00 after first acreditacion");
        verify(saldoVacacionesRepository).save(any(SaldoVacaciones.class));
    }

    // =========================================================================
    // G2.3 — acreditarDias() second acreditation accumulates
    // =========================================================================

    @Test
    @DisplayName("acreditarDias() must accumulate days: existing 10.0 + 10.0 = 20.0")
    void acreditar_segundaAcreditacion_acumula() {
        // GIVEN existing saldo with 10.0 dias generados
        SaldoVacaciones existing = new SaldoVacaciones();
        existing.setId(5L);
        existing.setEmpleado(empleado);
        existing.setDiasGenerados(new BigDecimal("10.00"));
        existing.setDiasDisfrutados(BigDecimal.ZERO);
        existing.setFechaUltimoCalculo(LocalDate.now().minusDays(50));

        when(empleadoRepository.findById(10L)).thenReturn(Optional.of(empleado));
        when(saldoVacacionesRepository.findByEmpleadoId(10L)).thenReturn(Optional.of(existing));

        ArgumentCaptor<SaldoVacaciones> captor = ArgumentCaptor.forClass(SaldoVacaciones.class);
        when(saldoVacacionesRepository.save(captor.capture())).thenReturn(existing);

        // WHEN — acreditar 10 more days
        service.acreditarDias(10L, new BigDecimal("10.00"));

        // THEN — 10 + 10 = 20
        SaldoVacaciones persisted = captor.getValue();
        assertEquals(0, new BigDecimal("20.00").compareTo(persisted.getDiasGenerados()),
                "diasGenerados must be 20.00 after second acreditacion (10 + 10)");
        assertEquals(LocalDate.now(), persisted.getFechaUltimoCalculo(),
                "fechaUltimoCalculo must be updated to today");
    }

    // =========================================================================
    // G2.4 — descontarDias() with sufficient balance subtracts correctly
    // =========================================================================

    @Test
    @DisplayName("descontarDias() must subtract from diasDisfrutados when balance is sufficient")
    void descontar_saldoSuficiente_subtrae() {
        // GIVEN saldo with 10 generados, 0 disfrutados => 10 disponibles
        SaldoVacaciones saldo = new SaldoVacaciones();
        saldo.setId(5L);
        saldo.setEmpleado(empleado);
        saldo.setDiasGenerados(new BigDecimal("10.00"));
        saldo.setDiasDisfrutados(BigDecimal.ZERO);
        saldo.setFechaUltimoCalculo(LocalDate.now());

        when(saldoVacacionesRepository.findByEmpleadoId(10L)).thenReturn(Optional.of(saldo));

        ArgumentCaptor<SaldoVacaciones> captor = ArgumentCaptor.forClass(SaldoVacaciones.class);
        when(saldoVacacionesRepository.save(captor.capture())).thenReturn(saldo);

        // WHEN
        service.descontarDias(10L, new BigDecimal("5.00"));

        // THEN
        SaldoVacaciones persisted = captor.getValue();
        assertEquals(0, new BigDecimal("5.00").compareTo(persisted.getDiasDisfrutados()),
                "diasDisfrutados must be 5.00 after descontar 5 from 10 available");
        verify(saldoVacacionesRepository).save(any(SaldoVacaciones.class));
    }

    // =========================================================================
    // G2.5 — descontarDias() throws when balance insufficient
    // =========================================================================

    @Test
    @DisplayName("descontarDias() must throw IllegalArgumentException when dias > diasDisponibles")
    void descontar_saldoInsuficiente_throwsIllegalArgument() {
        // GIVEN saldo with 10 generados, 0 disfrutados => 10 disponibles
        SaldoVacaciones saldo = new SaldoVacaciones();
        saldo.setId(5L);
        saldo.setEmpleado(empleado);
        saldo.setDiasGenerados(new BigDecimal("10.00"));
        saldo.setDiasDisfrutados(BigDecimal.ZERO);
        saldo.setFechaUltimoCalculo(LocalDate.now());

        when(saldoVacacionesRepository.findByEmpleadoId(10L)).thenReturn(Optional.of(saldo));

        // WHEN / THEN — try to descontar 15 when only 10 available
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.descontarDias(10L, new BigDecimal("15.00")));

        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("saldo") || msg.contains("dias") || msg.contains("disponible") || msg.contains("insuficiente"),
                "Error must mention insufficient balance. Got: " + ex.getMessage());
        verify(saldoVacacionesRepository, never()).save(any());
    }
}
