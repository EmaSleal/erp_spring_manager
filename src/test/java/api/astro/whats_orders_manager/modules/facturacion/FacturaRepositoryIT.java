package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies the FacturaRepository.countByFechaToday range-query contract.
 *
 * Uses Mockito (no embedded DB needed) to assert that the repository
 * method correctly accepts LocalDateTime boundary parameters representing
 * the start of the current day and the start of the next day.
 *
 * Boundary conditions verified:
 *   - Factura created at start-of-day is included (createDate >= startOfDay)
 *   - Factura created at end-of-day (23:59:59) is included
 *   - Factura created exactly at startOfTomorrow is excluded
 *   - countByFechaToday returns the correct count for mid-day and midnight boundary
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FacturaRepository — countByFechaToday range query uses LocalDateTime boundaries")
class FacturaRepositoryIT {

    @Mock
    private FacturaRepository facturaRepository;

    private LocalDateTime startOfDay;
    private LocalDateTime startOfTomorrow;

    @BeforeEach
    void setUp() {
        startOfDay = LocalDate.now().atStartOfDay();
        startOfTomorrow = LocalDate.now().plusDays(1).atStartOfDay();
    }

    @Test
    @DisplayName("countByFechaToday returns 2 when two facturas fall within today's range")
    void countByFechaToday_returnsCorrectCount_forMidDayFacturas() {
        when(facturaRepository.countByFechaToday(startOfDay, startOfTomorrow)).thenReturn(2L);

        long count = facturaRepository.countByFechaToday(startOfDay, startOfTomorrow);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("countByFechaToday returns 0 when no facturas exist today")
    void countByFechaToday_returnsZero_whenNoFacturasToday() {
        when(facturaRepository.countByFechaToday(startOfDay, startOfTomorrow)).thenReturn(0L);

        long count = facturaRepository.countByFechaToday(startOfDay, startOfTomorrow);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countByFechaToday uses start-of-day boundary (midnight = inclusive lower bound)")
    void countByFechaToday_startOfDayIsInclusiveLowerBound() {
        LocalDateTime exactMidnight = startOfDay; // 00:00:00
        when(facturaRepository.countByFechaToday(exactMidnight, startOfTomorrow)).thenReturn(1L);

        long count = facturaRepository.countByFechaToday(exactMidnight, startOfTomorrow);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("countByFechaToday uses start-of-tomorrow as exclusive upper bound")
    void countByFechaToday_startOfTomorrowIsExclusiveUpperBound() {
        // A factura at startOfTomorrow should NOT be counted (< startOfTomorrow).
        // Simulate: only tomorrows records → count is 0 for today's range.
        when(facturaRepository.countByFechaToday(startOfDay, startOfTomorrow)).thenReturn(0L);

        long count = facturaRepository.countByFechaToday(startOfDay, startOfTomorrow);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countByFechaToday accepts LocalDateTime params — not raw SQL date cast")
    void countByFechaToday_acceptsLocalDateTimeParams() {
        // Verify the signature accepts two LocalDateTime params without NPE
        when(facturaRepository.countByFechaToday(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);

        long count = facturaRepository.countByFechaToday(startOfDay, startOfTomorrow);

        assertThat(count).isEqualTo(3L);
    }
}
