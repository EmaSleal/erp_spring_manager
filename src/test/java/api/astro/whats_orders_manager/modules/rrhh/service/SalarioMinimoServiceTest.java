package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.SalarioMinimo;
import api.astro.whats_orders_manager.modules.rrhh.repository.SalarioMinimoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.SalarioMinimoServiceImpl;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalarioMinimoServiceImpl.
 *
 * RED phase — written before implementation.
 *
 * Covers:
 * - findVigenteByCategoria() returns vigente record when it exists
 * - findVigenteByCategoria() throws business exception when no vigente record
 * - findAll() delegates to repository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalarioMinimoServiceImpl — unit tests")
class SalarioMinimoServiceTest {

    @Mock
    private SalarioMinimoRepository salarioMinimoRepository;

    private SalarioMinimoService service;

    @BeforeEach
    void setUp() {
        service = new SalarioMinimoServiceImpl(salarioMinimoRepository);
    }

    // =========================================================================
    // findVigenteByCategoria() — vigente record exists
    // =========================================================================

    @Test
    @DisplayName("findVigenteByCategoria() must return record when vigente exists for categoria and date")
    void findVigenteByCategoria_vigente_returnsRecord() {
        // GIVEN TONC has vigenciaDesde=2026-01-01, vigenciaHasta=2026-06-30
        LocalDate fecha = LocalDate.of(2026, 3, 15);

        SalarioMinimo salario = buildSalario("TONC", "Trabajador No Calificado",
                new BigDecimal("373092.30"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30));

        when(salarioMinimoRepository.findVigenteByCategoria("TONC", fecha))
                .thenReturn(Optional.of(salario));

        // WHEN
        SalarioMinimo result = service.findVigenteByCategoria("TONC", fecha);

        // THEN
        assertNotNull(result);
        assertEquals("TONC", result.getCategoria());
        assertEquals(new BigDecimal("373092.30"), result.getMontoMensual());
        verify(salarioMinimoRepository).findVigenteByCategoria("TONC", fecha);
    }

    @Test
    @DisplayName("findVigenteByCategoria() must return correct record for a second different categoria (DOM)")
    void findVigenteByCategoria_categoriaDOM_retornaRegistroCorrecto() {
        LocalDate fecha = LocalDate.of(2026, 4, 1);

        SalarioMinimo salarioDOM = buildSalario("DOM", "Trabajo domestico",
                new BigDecimal("268731.31"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30));

        when(salarioMinimoRepository.findVigenteByCategoria("DOM", fecha))
                .thenReturn(Optional.of(salarioDOM));

        SalarioMinimo result = service.findVigenteByCategoria("DOM", fecha);

        assertNotNull(result);
        assertEquals("DOM", result.getCategoria());
        assertEquals(new BigDecimal("268731.31"), result.getMontoMensual());
        verify(salarioMinimoRepository).findVigenteByCategoria("DOM", fecha);
    }

    @Test
    @DisplayName("findVigenteByCategoria() must return TC (Trabajador Calificado) record with different amount")
    void findVigenteByCategoria_categoriaTC_retornaMontoDistinto() {
        LocalDate fecha = LocalDate.of(2026, 2, 20);

        SalarioMinimo salarioTC = buildSalario("TC", "Trabajador Calificado",
                new BigDecimal("550000.00"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31));

        when(salarioMinimoRepository.findVigenteByCategoria("TC", fecha))
                .thenReturn(Optional.of(salarioTC));

        SalarioMinimo result = service.findVigenteByCategoria("TC", fecha);

        assertNotNull(result);
        assertEquals("TC", result.getCategoria());
        assertEquals(new BigDecimal("550000.00"), result.getMontoMensual());
        verify(salarioMinimoRepository).findVigenteByCategoria("TC", fecha);
    }

    // =========================================================================
    // findVigenteByCategoria() — no vigente record → throws
    // =========================================================================

    @Test
    @DisplayName("findVigenteByCategoria() must throw business exception when no vigente record exists")
    void findVigenteByCategoria_noRecord_throwsException() {
        LocalDate fecha = LocalDate.of(2026, 3, 15);

        when(salarioMinimoRepository.findVigenteByCategoria("XYZ", fecha))
                .thenReturn(Optional.empty());

        // WHEN / THEN — any runtime exception is acceptable; spec says "business exception"
        Exception ex = assertThrows(Exception.class,
                () -> service.findVigenteByCategoria("XYZ", fecha));

        String msg = ex.getMessage().toLowerCase();
        assertTrue(
            msg.contains("xyz") || msg.contains("salario") || msg.contains("categor") || msg.contains("vigente"),
            "Exception message must reference the categoria or indicate no vigente record. Got: " + ex.getMessage()
        );

        verify(salarioMinimoRepository).findVigenteByCategoria("XYZ", fecha);
    }

    @Test
    @DisplayName("findVigenteByCategoria() must also throw for a second distinct unknown categoria")
    void findVigenteByCategoria_otraCategoriaDesconocida_throwsException() {
        LocalDate fecha = LocalDate.of(2026, 5, 10);

        when(salarioMinimoRepository.findVigenteByCategoria("INVALIDA", fecha))
                .thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> service.findVigenteByCategoria("INVALIDA", fecha));
        verify(salarioMinimoRepository).findVigenteByCategoria("INVALIDA", fecha);
    }

    // =========================================================================
    // findVigenteByCategoria() — vigenciaHasta=null (indefinite)
    // =========================================================================

    @Test
    @DisplayName("findVigenteByCategoria() must return record when vigenciaHasta is null (indefinitely vigente)")
    void findVigenteByCategoria_indefinite_returnsRecord() {
        LocalDate fecha = LocalDate.of(2030, 1, 1);

        SalarioMinimo salario = buildSalario("TONC", "Trabajador No Calificado",
                new BigDecimal("400000.00"),
                LocalDate.of(2028, 7, 1),
                null);

        when(salarioMinimoRepository.findVigenteByCategoria("TONC", fecha))
                .thenReturn(Optional.of(salario));

        SalarioMinimo result = service.findVigenteByCategoria("TONC", fecha);

        assertNotNull(result);
        assertNull(result.getVigenciaHasta(), "vigenciaHasta must be null for indefinite record");
    }

    // =========================================================================
    // findAll()
    // =========================================================================

    @Test
    @DisplayName("findAll() must delegate to repository")
    void findAll_delegatesToRepository() {
        SalarioMinimo s = buildSalario("DOM", "Trabajo domestico",
                new BigDecimal("268731.31"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        when(salarioMinimoRepository.findAll()).thenReturn(List.of(s));

        List<SalarioMinimo> result = service.findAll();

        assertEquals(1, result.size());
        verify(salarioMinimoRepository).findAll();
    }

    @Test
    @DisplayName("findAll() must return all categories when repository has multiple records")
    void findAll_multiplesCategories_returnsAll() {
        SalarioMinimo tonc = buildSalario("TONC", "Trabajador No Calificado",
                new BigDecimal("373092.30"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        SalarioMinimo dom = buildSalario("DOM", "Trabajo domestico",
                new BigDecimal("268731.31"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        SalarioMinimo tc = buildSalario("TC", "Trabajador Calificado",
                new BigDecimal("550000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        when(salarioMinimoRepository.findAll()).thenReturn(List.of(tonc, dom, tc));

        List<SalarioMinimo> result = service.findAll();

        assertEquals(3, result.size());
        verify(salarioMinimoRepository).findAll();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SalarioMinimo buildSalario(String categoria, String descripcion,
                                        BigDecimal monto, LocalDate desde, LocalDate hasta) {
        SalarioMinimo s = new SalarioMinimo();
        s.setId(1L);
        s.setCategoria(categoria);
        s.setDescripcionCategoria(descripcion);
        s.setMontoMensual(monto);
        s.setVigenciaDesde(desde);
        s.setVigenciaHasta(hasta);
        s.setCreatedAt(LocalDateTime.now());
        return s;
    }
}
