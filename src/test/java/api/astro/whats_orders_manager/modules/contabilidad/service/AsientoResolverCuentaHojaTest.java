package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-014 (ADR-017 R1) — resolverCuenta, used by every automatic entry, only accepts leaf accounts:
 * active, accepting movements and without subaccounts. Otherwise it throws IllegalStateException
 * naming the key and the account code, before anything is saved.
 *
 * Exercised through generarAsientoNomina: its first key is NOMINA_GASTO_SUELDOS.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsientoContableService.resolverCuenta — solo cuentas hoja (ADR-017)")
class AsientoResolverCuentaHojaTest {

    private static final String CLAVE = "NOMINA_GASTO_SUELDOS";

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private AsientoContableService service;

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);
    }

    @Test
    @DisplayName("cuenta con hijas (agrupadora): IllegalStateException con clave y código, sin asiento guardado")
    void cuentaConHijas_lanzaIllegalState() {
        CuentaContable agrupadora = cuenta(5L, "5.1.01", true, true);
        mapear(agrupadora);
        when(cuentaRepository.countSubcuentas(5L)).thenReturn(1L);

        assertThatThrownBy(() -> service.generarAsientoNomina(nomina()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(CLAVE)
                .hasMessageContaining("5.1.01")
                .hasMessageContaining("subcuentas");

        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cuenta inactiva: IllegalStateException con clave y código, sin asiento guardado")
    void cuentaInactiva_lanzaIllegalState() {
        mapear(cuenta(6L, "5.1.02.002", false, true));

        assertThatThrownBy(() -> service.generarAsientoNomina(nomina()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(CLAVE)
                .hasMessageContaining("5.1.02.002")
                .hasMessageContaining("inactiva");

        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cuenta que no acepta movimientos: IllegalStateException con clave y código, sin asiento guardado")
    void cuentaSinMovimientos_lanzaIllegalState() {
        mapear(cuenta(28L, "5.1.02", true, false));

        assertThatThrownBy(() -> service.generarAsientoNomina(nomina()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(CLAVE)
                .hasMessageContaining("5.1.02")
                .hasMessageContaining("no acepta movimientos");

        verify(asientoRepository, never()).save(any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void mapear(CuentaContable cuenta) {
        when(parametroContableRepository.findByClave(CLAVE))
                .thenReturn(Optional.of(ParametroContable.builder().clave(CLAVE).cuentaContable(cuenta).build()));
    }

    private static CuentaContable cuenta(Long id, String codigo, boolean activa, boolean aceptaMovimientos) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setActiva(activa);
        c.setAceptaMovimientos(aceptaMovimientos);
        return c;
    }

    private static Nomina nomina() {
        Nomina nomina = Nomina.builder()
                .id(1L)
                .numero("NOM-2026-0001")
                .periodoInicio(LocalDate.of(2026, 7, 1))
                .periodoFin(LocalDate.of(2026, 7, 31))
                .tipo(TipoNomina.MENSUAL)
                .estado(EstadoNomina.APROBADA)
                .totalBruto(new BigDecimal("1000000.00"))
                .totalCcssPatronal(new BigDecimal("268300.00"))
                .totalNeto(new BigDecimal("881700.00"))
                .build();
        nomina.setDetalles(new ArrayList<>());
        return nomina;
    }
}
