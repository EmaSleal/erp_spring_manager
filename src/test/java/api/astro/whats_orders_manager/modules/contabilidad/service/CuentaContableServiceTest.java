package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-014 (ADR-017 R1/R3) — hierarchy rules of the chart of accounts. Children are detected through
 * the cuenta_padre_id FK (CuentaContableRepository.countSubcuentas).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CuentaContableService — reglas de jerarquía (ADR-017)")
class CuentaContableServiceTest {

    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private ParametroContableRepository parametroRepository;

    private CuentaContableService service;

    @BeforeEach
    void setUp() {
        service = new CuentaContableService(cuentaRepository, detalleRepository, parametroRepository);
    }

    @Test
    @DisplayName("crear rechaza una hija bajo un padre con movimientos")
    void crear_hijaBajoPadreConMovimientos_rechaza() {
        CuentaContable padre = cuenta(5L, "5.1.01", true);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(padre));
        when(detalleRepository.contarMovimientosCuenta(5L)).thenReturn(3L);

        assertThatThrownBy(() -> service.crear(nueva("5.1.01.001", padre)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5.1.01")
                .hasMessageContaining("movimientos");
        verify(cuentaRepository, never()).save(any());
        assertThat(padre.getAceptaMovimientos()).isTrue();
    }

    @Test
    @DisplayName("crear rechaza el código 1.1.010 bajo 1.1.01 (el prefijo exige el punto)")
    void crear_codigoSinPuntoSeparador_rechaza() {
        CuentaContable padre = cuenta(1L, "1.1.01", false);
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(padre));

        assertThatThrownBy(() -> service.crear(nueva("1.1.010", padre)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1.1.01.");
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear acepta una hija válida y el padre deja de aceptar movimientos")
    void crear_hijaValida_padreQuedaAgrupador() {
        CuentaContable padre = cuenta(5L, "5.1.01", true);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(padre));
        when(detalleRepository.contarMovimientosCuenta(5L)).thenReturn(0L);
        when(cuentaRepository.save(any(CuentaContable.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaContable hija = service.crear(nueva("5.1.01.001", padre));

        assertThat(hija.getCuentaPadre()).isSameAs(padre);
        assertThat(padre.getAceptaMovimientos()).isFalse();
    }

    @Test
    @DisplayName("actualizar rechaza aceptaMovimientos = true en una cuenta con hijas")
    void actualizar_aceptaMovimientosConHijas_rechaza() {
        CuentaContable agrupadora = cuenta(1L, "1.1.01", false);
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(agrupadora));
        when(cuentaRepository.countSubcuentas(1L)).thenReturn(5L);
        CuentaContable cambios = cuenta(null, "1.1.01", true);

        assertThatThrownBy(() -> service.actualizar(1L, cambios))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1.1.01")
                .hasMessageContaining("subcuentas");
        verify(cuentaRepository, never()).save(any());
        assertThat(agrupadora.getAceptaMovimientos()).isFalse();
    }

    @Test
    @DisplayName("actualizar rechaza desactivar una cuenta usada por un parámetro contable")
    void actualizar_desactivarCuentaUsadaPorClave_rechaza() {
        CuentaContable caja = cuenta(16L, "1.1.01.001", true);
        when(cuentaRepository.findById(16L)).thenReturn(Optional.of(caja));
        when(parametroRepository.findClavesByCuentaId(16L))
                .thenReturn(List.of("COBRO_CAJA_EFECTIVO", "CPP_BANCO_EFECTIVO"));
        CuentaContable cambios = cuenta(null, "1.1.01.001", true);
        cambios.setActiva(false);

        assertThatThrownBy(() -> service.actualizar(16L, cambios))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1.1.01.001")
                .hasMessageContaining("COBRO_CAJA_EFECTIVO, CPP_BANCO_EFECTIVO");
        verify(cuentaRepository, never()).save(any());
        assertThat(caja.getActiva()).isTrue();
    }

    @Test
    @DisplayName("cambiarEstado rechaza desactivar una cuenta usada por un parámetro contable")
    void cambiarEstado_desactivarCuentaUsadaPorClave_rechaza() {
        CuentaContable caja = cuenta(16L, "1.1.01.001", true);
        when(cuentaRepository.findById(16L)).thenReturn(Optional.of(caja));
        when(parametroRepository.findClavesByCuentaId(16L)).thenReturn(List.of("COBRO_CAJA_EFECTIVO"));

        assertThatThrownBy(() -> service.cambiarEstado(16L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COBRO_CAJA_EFECTIVO");
        verify(cuentaRepository, never()).save(any());
        assertThat(caja.getActiva()).isTrue();
    }

    @Test
    @DisplayName("eliminar la única hija: el padre vuelve a aceptar movimientos")
    void eliminar_unicaHija_padreAceptaMovimientos() {
        CuentaContable padre = cuenta(40L, "4.1.01", false);
        CuentaContable hija = cuenta(41L, "4.1.01.001", true);
        padre.getSubcuentas().add(hija);
        hija.setCuentaPadre(padre);
        when(cuentaRepository.findById(41L)).thenReturn(Optional.of(hija));
        when(cuentaRepository.countSubcuentas(41L)).thenReturn(0L);
        when(detalleRepository.contarMovimientosCuenta(41L)).thenReturn(0L);
        when(cuentaRepository.countSubcuentas(40L)).thenReturn(1L);

        service.eliminar(41L);

        assertThat(padre.getAceptaMovimientos()).isTrue();
        assertThat(padre.getSubcuentas()).isEmpty();
        verify(cuentaRepository).save(padre);
        verify(cuentaRepository).delete(hija);
    }

    @Test
    @DisplayName("eliminar una hija con hermanas: el padre sigue siendo agrupador")
    void eliminar_hijaConHermanas_padreSigueAgrupador() {
        CuentaContable padre = cuenta(40L, "4.1.01", false);
        CuentaContable hija = cuenta(41L, "4.1.01.001", true);
        CuentaContable hermana = cuenta(42L, "4.1.01.002", true);
        padre.getSubcuentas().add(hija);
        padre.getSubcuentas().add(hermana);
        hija.setCuentaPadre(padre);
        hermana.setCuentaPadre(padre);
        when(cuentaRepository.findById(41L)).thenReturn(Optional.of(hija));
        when(cuentaRepository.countSubcuentas(41L)).thenReturn(0L);
        when(detalleRepository.contarMovimientosCuenta(41L)).thenReturn(0L);
        when(cuentaRepository.countSubcuentas(40L)).thenReturn(2L);

        service.eliminar(41L);

        assertThat(padre.getAceptaMovimientos()).isFalse();
        verify(cuentaRepository).delete(hija);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static CuentaContable cuenta(Long id, String codigo, boolean aceptaMovimientos) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setTipo(TipoCuenta.ACTIVO);
        c.setActiva(true);
        c.setAceptaMovimientos(aceptaMovimientos);
        return c;
    }

    private static CuentaContable nueva(String codigo, CuentaContable padre) {
        CuentaContable c = cuenta(null, codigo, true);
        CuentaContable refPadre = new CuentaContable();
        refPadre.setIdCuenta(padre.getIdCuenta());
        c.setCuentaPadre(refPadre);
        return c;
    }
}
