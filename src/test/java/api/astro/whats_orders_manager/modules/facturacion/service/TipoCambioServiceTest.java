package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TipoCambioService — unit tests")
class TipoCambioServiceTest {

    @Mock
    private TipoCambioRepository repo;

    @InjectMocks
    private TipoCambioService service;

    private TipoCambio usdRate;

    @BeforeEach
    void setUp() {
        usdRate = TipoCambio.builder()
                .id(1L)
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.USD)
                .fecha(LocalDate.now())
                .tasaCompra(new BigDecimal("530.00000"))
                .tasaVenta(new BigDecimal("532.15000"))
                .fuente(FuenteTipoCambio.MANUAL)
                .activo(true)
                .build();
    }

    // =========================================================================
    // getTasaActual
    // =========================================================================

    @Test
    @DisplayName("getTasaActual returns the latest active rate when one exists")
    void getTasaActual_whenRateExists_returnsIt() {
        when(repo.findFirstByMonedaDestinoAndActivoTrueOrderByFechaDesc(MonedaFE.USD))
                .thenReturn(Optional.of(usdRate));

        Optional<TipoCambio> result = service.getTasaActual(MonedaFE.USD);

        assertThat(result).isPresent();
        assertThat(result.get().getTasaVenta()).isEqualByComparingTo("532.15000");
    }

    @Test
    @DisplayName("getTasaActual returns empty when no active rate exists")
    void getTasaActual_whenNoRate_returnsEmpty() {
        when(repo.findFirstByMonedaDestinoAndActivoTrueOrderByFechaDesc(MonedaFE.EUR))
                .thenReturn(Optional.empty());

        Optional<TipoCambio> result = service.getTasaActual(MonedaFE.EUR);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // registrar — new rate (no prior entry for this date)
    // =========================================================================

    @Test
    @DisplayName("registrar saves a new rate when no entry exists for the same date and currency")
    void registrar_noConflict_savesNewRate() {
        doNothing().when(repo).deactivateExisting(usdRate.getFecha(), MonedaFE.USD);
        when(repo.save(any(TipoCambio.class))).thenReturn(usdRate);

        TipoCambio saved = service.registrar(usdRate);

        assertThat(saved.getId()).isEqualTo(1L);
        verify(repo, never()).save(argThat(tc -> !tc.isActivo())); // no deactivation needed
        verify(repo, times(1)).save(usdRate);
    }

    @Test
    @DisplayName("registrar deactivates the old rate and saves the new one when a conflict exists")
    void registrar_conflictExists_deactivatesOldAndSavesNew() {
        TipoCambio existing = TipoCambio.builder()
                .id(99L)
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.USD)
                .fecha(LocalDate.now())
                .tasaCompra(new BigDecimal("525.00000"))
                .tasaVenta(new BigDecimal("527.00000"))
                .fuente(FuenteTipoCambio.MANUAL)
                .activo(true)
                .build();

        doNothing().when(repo).deactivateExisting(usdRate.getFecha(), MonedaFE.USD);
        when(repo.save(any(TipoCambio.class))).thenReturn(usdRate);

        service.registrar(usdRate);

        verify(repo).deactivateExisting(usdRate.getFecha(), MonedaFE.USD);
        verify(repo, times(1)).save(usdRate);
    }

    // =========================================================================
    // listarPorMoneda
    // =========================================================================

    @Test
    @DisplayName("listarPorMoneda delegates to repo and returns the list")
    void listarPorMoneda_returnsList() {
        TipoCambio second = TipoCambio.builder()
                .id(2L)
                .monedaDestino(MonedaFE.USD)
                .fecha(LocalDate.now().minusDays(1))
                .tasaCompra(new BigDecimal("528.00000"))
                .tasaVenta(new BigDecimal("530.00000"))
                .fuente(FuenteTipoCambio.MANUAL)
                .activo(false)
                .build();

        when(repo.findByMonedaDestinoOrderByFechaDesc(MonedaFE.USD))
                .thenReturn(List.of(usdRate, second));

        List<TipoCambio> result = service.listarPorMoneda(MonedaFE.USD);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("listarPorMoneda returns empty list when currency has no history")
    void listarPorMoneda_noHistory_returnsEmptyList() {
        when(repo.findByMonedaDestinoOrderByFechaDesc(MonedaFE.EUR))
                .thenReturn(List.of());

        List<TipoCambio> result = service.listarPorMoneda(MonedaFE.EUR);

        assertThat(result).isEmpty();
    }
}
