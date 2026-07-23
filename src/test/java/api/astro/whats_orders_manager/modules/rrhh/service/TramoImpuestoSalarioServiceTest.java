package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;
import api.astro.whats_orders_manager.modules.rrhh.repository.TramoImpuestoSalarioRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.TramoImpuestoSalarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TramoImpuestoSalarioServiceImpl.
 *
 * RED phase — written before implementation.
 *
 * Covers:
 * - findByAnioVigencia(2026) returns rows ordered by limiteInferior ASC
 * - first row must have limiteInferior=0 and non-null tax credits
 * - findByAnioVigencia() with no results returns empty list
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TramoImpuestoSalarioServiceImpl — unit tests")
class TramoImpuestoSalarioServiceTest {

    @Mock
    private TramoImpuestoSalarioRepository tramoRepository;

    private TramoImpuestoSalarioService service;

    @BeforeEach
    void setUp() {
        service = new TramoImpuestoSalarioServiceImpl(tramoRepository);
    }

    // =========================================================================
    // findByAnioVigencia() — ordered brackets for 2026
    // =========================================================================

    @Test
    @DisplayName("findByAnioVigencia(2026) must return 5 rows ordered by limiteInferior ASC")
    void findByAnioVigencia_2026_returnsOrderedBrackets() {
        // GIVEN 5 tramos for 2026 returned in ascending limiteInferior order
        List<TramoImpuestoSalario> tramos = buildTramos2026();

        when(tramoRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2026))
                .thenReturn(tramos);

        // WHEN
        List<TramoImpuestoSalario> result = service.findByAnioVigencia(2026);

        // THEN
        assertEquals(5, result.size(), "Must return all 5 brackets for 2026");

        // Verify ascending order
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(
                result.get(i).getLimiteInferior()
                      .compareTo(result.get(i + 1).getLimiteInferior()) <= 0,
                "Brackets must be ordered by limiteInferior ASC"
            );
        }

        // First bracket must have limiteInferior = 0
        assertEquals(BigDecimal.ZERO.setScale(2), result.get(0).getLimiteInferior().setScale(2),
                "First bracket limiteInferior must be 0.00");

        verify(tramoRepository).findByAnioVigenciaOrderByLimiteInferiorAsc(2026);
    }

    @Test
    @DisplayName("findByAnioVigencia(2026) — first bracket must have non-null tax credits")
    void findByAnioVigencia_2026_firstBracketHasCredits() {
        List<TramoImpuestoSalario> tramos = buildTramos2026();

        when(tramoRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2026))
                .thenReturn(tramos);

        List<TramoImpuestoSalario> result = service.findByAnioVigencia(2026);

        TramoImpuestoSalario firstBracket = result.get(0);
        assertNotNull(firstBracket.getCreditoPorHijo(),
                "First bracket (exento) must have creditoPorHijo");
        assertNotNull(firstBracket.getCreditoPorConyuge(),
                "First bracket (exento) must have creditoPorConyuge");
        assertEquals(new BigDecimal("1710.00"), firstBracket.getCreditoPorHijo());
        assertEquals(new BigDecimal("2590.00"), firstBracket.getCreditoPorConyuge());
    }

    @Test
    @DisplayName("findByAnioVigencia() with no results returns empty list")
    void findByAnioVigencia_noResults_returnsEmpty() {
        when(tramoRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(1999))
                .thenReturn(List.of());

        List<TramoImpuestoSalario> result = service.findByAnioVigencia(1999);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByAnioVigencia(2027) must return 3 rows with distinct limits ordered ASC")
    void findByAnioVigencia_2027_returnsOrderedBrackets() {
        // GIVEN 3 tramos for 2027 with different limits than 2026
        List<TramoImpuestoSalario> tramos2027 = List.of(
            buildTramo(2027, "0.00",        "960000.00", "0.0000", "1800.00", "2700.00"),
            buildTramo(2027, "960000.00",  "1400000.00", "0.1000", null,       null),
            buildTramo(2027, "1400000.00",  null,        "0.1500", null,       null)
        );

        when(tramoRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2027))
                .thenReturn(tramos2027);

        List<TramoImpuestoSalario> result = service.findByAnioVigencia(2027);

        assertEquals(3, result.size(), "Must return 3 brackets for 2027");

        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(
                result.get(i).getLimiteInferior()
                      .compareTo(result.get(i + 1).getLimiteInferior()) <= 0,
                "Brackets must be ordered by limiteInferior ASC"
            );
        }

        assertEquals(BigDecimal.ZERO.setScale(2), result.get(0).getLimiteInferior().setScale(2),
                "First 2027 bracket must start at 0");
        assertEquals(new BigDecimal("1800.00"), result.get(0).getCreditoPorHijo());
        assertEquals(new BigDecimal("2700.00"), result.get(0).getCreditoPorConyuge());

        verify(tramoRepository).findByAnioVigenciaOrderByLimiteInferiorAsc(2027);
    }

    @Test
    @DisplayName("findByAnioVigencia() must isolate calls — 2026 and 2027 must use separate repository calls")
    void findByAnioVigencia_dosanosDistintos_usanLlamadasSeparadas() {
        when(tramoRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2026))
                .thenReturn(buildTramos2026());
        when(tramoRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(2027))
                .thenReturn(List.of(buildTramo(2027, "0.00", null, "0.0000", "1800.00", "2700.00")));

        List<TramoImpuestoSalario> result2026 = service.findByAnioVigencia(2026);
        List<TramoImpuestoSalario> result2027 = service.findByAnioVigencia(2027);

        assertEquals(5, result2026.size());
        assertEquals(1, result2027.size());

        verify(tramoRepository).findByAnioVigenciaOrderByLimiteInferiorAsc(2026);
        verify(tramoRepository).findByAnioVigenciaOrderByLimiteInferiorAsc(2027);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<TramoImpuestoSalario> buildTramos2026() {
        return List.of(
            buildTramo(2026, "0.00",       "918000.00", "0.0000", "1710.00", "2590.00"),
            buildTramo(2026, "918000.00", "1347000.00", "0.1000", null,       null),
            buildTramo(2026, "1347000.00","2364000.00", "0.1500", null,       null),
            buildTramo(2026, "2364000.00","4727000.00", "0.2000", null,       null),
            buildTramo(2026, "4727000.00", null,        "0.2500", null,       null)
        );
    }

    private TramoImpuestoSalario buildTramo(int anio, String inferior, String superior,
                                            String pct, String hijo, String conyuge) {
        TramoImpuestoSalario t = new TramoImpuestoSalario();
        t.setAnioVigencia(anio);
        t.setLimiteInferior(new BigDecimal(inferior));
        t.setLimiteSuperior(superior != null ? new BigDecimal(superior) : null);
        t.setPorcentaje(new BigDecimal(pct));
        t.setCreditoPorHijo(hijo != null ? new BigDecimal(hijo) : null);
        t.setCreditoPorConyuge(conyuge != null ? new BigDecimal(conyuge) : null);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}
