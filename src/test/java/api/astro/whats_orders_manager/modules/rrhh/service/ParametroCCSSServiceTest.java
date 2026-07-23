package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroCCSS;
import api.astro.whats_orders_manager.modules.rrhh.repository.ParametroCCSSRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.ParametroCCSSServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ParametroCCSSServiceImpl.
 *
 * RED phase — written before implementation.
 *
 * Covers:
 * - findVigenteByFecha() returns record whose vigenciaDesde <= fecha <= vigenciaHasta
 * - findVigenteByFecha() returns record when vigenciaHasta is null (indefinite)
 * - findVigenteByFecha() throws when no vigente record found
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParametroCCSSServiceImpl — unit tests")
class ParametroCCSSServiceTest {

    @Mock
    private ParametroCCSSRepository parametroCCSSRepository;

    private ParametroCCSSService service;

    @BeforeEach
    void setUp() {
        service = new ParametroCCSSServiceImpl(parametroCCSSRepository);
    }

    // =========================================================================
    // findVigenteByFecha() — date within bounded range
    // =========================================================================

    @Test
    @DisplayName("findVigenteByFecha() must return record when query date falls within vigenciaDesde..vigenciaHasta")
    void findVigenteByFecha_dateInRange_returnsRecord() {
        // GIVEN record with vigenciaDesde=2026-01-01 and vigenciaHasta=2028-12-31
        LocalDate fecha = LocalDate.of(2027, 6, 15);

        ParametroCCSS params = buildSampleParametro(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2028, 12, 31));

        when(parametroCCSSRepository.findVigenteByFecha(fecha))
                .thenReturn(Optional.of(params));

        // WHEN
        ParametroCCSS result = service.findVigenteByFecha(fecha);

        // THEN
        assertNotNull(result);
        assertEquals(new BigDecimal("0.1083"), result.getPorcentajeObrero());
        assertEquals(new BigDecimal("0.2683"), result.getPorcentajePatronal());
        verify(parametroCCSSRepository).findVigenteByFecha(fecha);
    }

    // =========================================================================
    // findVigenteByFecha() — indefinite vigencia (vigenciaHasta=null)
    // =========================================================================

    @Test
    @DisplayName("findVigenteByFecha() must return record when vigenciaHasta is null (indefinitely vigente)")
    void findVigenteByFecha_vigenciaHastaNull_returnsRecord() {
        // GIVEN record with vigenciaDesde=2026-01-01 and vigenciaHasta=NULL
        LocalDate fecha = LocalDate.of(2030, 1, 1);

        ParametroCCSS params = buildSampleParametro(
                LocalDate.of(2026, 1, 1),
                null);

        when(parametroCCSSRepository.findVigenteByFecha(fecha))
                .thenReturn(Optional.of(params));

        // WHEN
        ParametroCCSS result = service.findVigenteByFecha(fecha);

        // THEN
        assertNotNull(result);
        assertNull(result.getVigenciaHasta(), "vigenciaHasta must be null for indefinite record");
        verify(parametroCCSSRepository).findVigenteByFecha(fecha);
    }

    // =========================================================================
    // findVigenteByFecha() — no vigente record
    // =========================================================================

    @Test
    @DisplayName("findVigenteByFecha() must throw when no vigente record exists for the date")
    void findVigenteByFecha_noRecord_throwsException() {
        LocalDate fecha = LocalDate.of(2020, 1, 1);

        when(parametroCCSSRepository.findVigenteByFecha(fecha))
                .thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.findVigenteByFecha(fecha));
        verify(parametroCCSSRepository).findVigenteByFecha(fecha);
    }

    // =========================================================================
    // findAll()
    // =========================================================================

    @Test
    @DisplayName("findVigenteByFecha() must return record with different rates for a different fecha")
    void findVigenteByFecha_diferenteFecha_retornaTasas2024() {
        // GIVEN a different record valid during 2024 with distinct rates
        LocalDate fecha = LocalDate.of(2024, 3, 10);

        ParametroCCSS params2024 = new ParametroCCSS();
        params2024.setId(2L);
        params2024.setPorcentajeObrero(new BigDecimal("0.1083"));
        params2024.setPorcentajePatronal(new BigDecimal("0.2633"));  // distinct value
        params2024.setPorcentajeSem(new BigDecimal("0.0500"));       // distinct value
        params2024.setPorcentajeIvmObrero(new BigDecimal("0.0433"));
        params2024.setPorcentajeBpObrero(new BigDecimal("0.0100"));
        params2024.setPorcentajeFcl(new BigDecimal("0.0150"));
        params2024.setPorcentajeRop(new BigDecimal("0.0200"));
        params2024.setBaseMinimaContributivaSem(new BigDecimal("310000.00"));
        params2024.setBaseMinimaContributivaIvm(new BigDecimal("290000.00"));
        params2024.setVigenciaDesde(LocalDate.of(2024, 1, 1));
        params2024.setVigenciaHasta(LocalDate.of(2024, 12, 31));
        params2024.setCreatedAt(LocalDateTime.now());

        when(parametroCCSSRepository.findVigenteByFecha(fecha))
                .thenReturn(Optional.of(params2024));

        ParametroCCSS result = service.findVigenteByFecha(fecha);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.2633"), result.getPorcentajePatronal());
        assertEquals(new BigDecimal("310000.00"), result.getBaseMinimaContributivaSem());
        verify(parametroCCSSRepository).findVigenteByFecha(fecha);
    }

    @Test
    @DisplayName("findVigenteByFecha() must throw for a second different missing-date scenario")
    void findVigenteByFecha_otraFechaNoExistente_throwsException() {
        LocalDate fecha = LocalDate.of(2015, 6, 1);

        when(parametroCCSSRepository.findVigenteByFecha(fecha))
                .thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.findVigenteByFecha(fecha));
        verify(parametroCCSSRepository).findVigenteByFecha(fecha);
    }

    @Test
    @DisplayName("findAll() must delegate to repository")
    void findAll_delegatesToRepository() {
        when(parametroCCSSRepository.findAll()).thenReturn(java.util.List.of());
        java.util.List<ParametroCCSS> result = service.findAll();
        assertNotNull(result);
        verify(parametroCCSSRepository).findAll();
    }

    @Test
    @DisplayName("findAll() must return all records when repository has multiple entries")
    void findAll_multiplesRegistros_returnsAll() {
        ParametroCCSS p1 = buildSampleParametro(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        ParametroCCSS p2 = buildSampleParametro(
                LocalDate.of(2026, 1, 1), null);

        when(parametroCCSSRepository.findAll()).thenReturn(java.util.List.of(p1, p2));

        java.util.List<ParametroCCSS> result = service.findAll();

        assertEquals(2, result.size());
        verify(parametroCCSSRepository).findAll();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ParametroCCSS buildSampleParametro(LocalDate desde, LocalDate hasta) {
        ParametroCCSS p = new ParametroCCSS();
        p.setId(1L);
        p.setPorcentajeObrero(new BigDecimal("0.1083"));
        p.setPorcentajePatronal(new BigDecimal("0.2683"));
        p.setPorcentajeSem(new BigDecimal("0.0550"));
        p.setPorcentajeIvmObrero(new BigDecimal("0.0433"));
        p.setPorcentajeBpObrero(new BigDecimal("0.0100"));
        p.setPorcentajeFcl(new BigDecimal("0.0150"));
        p.setPorcentajeRop(new BigDecimal("0.0200"));
        p.setBaseMinimaContributivaSem(new BigDecimal("333328.00"));
        p.setBaseMinimaContributivaIvm(new BigDecimal("311990.00"));
        p.setVigenciaDesde(desde);
        p.setVigenciaHasta(hasta);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }
}
