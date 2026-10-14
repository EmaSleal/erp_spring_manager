package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-014 (ADR-017 R1) — a parametro contable can only point to a leaf account (active, accepting
 * movements, without subaccounts), and the key is normalized before looking for duplicates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParametroContableService — solo cuentas hoja y clave normalizada")
class ParametroContableServiceTest {

    @Mock private ParametroContableRepository parametroRepo;
    @Mock private CuentaContableRepository cuentaRepo;

    private ParametroContableService service;

    @BeforeEach
    void setUp() {
        service = new ParametroContableService(parametroRepo, cuentaRepo);
    }

    @Test
    @DisplayName("crear rechaza una cuenta agrupadora (con subcuentas)")
    void crear_cuentaAgrupadora_rechaza() {
        when(cuentaRepo.findById(1L)).thenReturn(Optional.of(cuenta(1L, "1.1.01", true, true)));
        when(cuentaRepo.countSubcuentas(1L)).thenReturn(5L);

        assertThatThrownBy(() -> service.crear("CPP_BANCO_EFECTIVO", "Caja", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1.1.01")
                .hasMessageContaining("subcuentas")
                .hasMessageContaining("CPP_BANCO_EFECTIVO");
        verify(parametroRepo, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza una cuenta inactiva")
    void crear_cuentaInactiva_rechaza() {
        when(cuentaRepo.findById(16L)).thenReturn(Optional.of(cuenta(16L, "1.1.01.001", false, true)));

        assertThatThrownBy(() -> service.crear("CPP_BANCO_EFECTIVO", "Caja", 16L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1.1.01.001")
                .hasMessageContaining("inactiva");
        verify(parametroRepo, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza una cuenta que no acepta movimientos")
    void crear_cuentaSinMovimientos_rechaza() {
        when(cuentaRepo.findById(28L)).thenReturn(Optional.of(cuenta(28L, "5.1.02", true, false)));

        assertThatThrownBy(() -> service.crear("NOMINA_GASTO_SUELDOS", "Sueldos", 28L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5.1.02")
                .hasMessageContaining("no acepta movimientos");
        verify(parametroRepo, never()).save(any());
    }

    @Test
    @DisplayName("crear acepta una hoja y guarda la clave normalizada")
    void crear_hoja_acepta() {
        CuentaContable hoja = cuenta(16L, "1.1.01.001", true, true);
        when(cuentaRepo.findById(16L)).thenReturn(Optional.of(hoja));
        when(cuentaRepo.countSubcuentas(16L)).thenReturn(0L);
        when(parametroRepo.save(any(ParametroContable.class))).thenAnswer(inv -> inv.getArgument(0));

        ParametroContable creado = service.crear("  cpp_banco_efectivo ", "Caja", 16L);

        assertThat(creado.getClave()).isEqualTo("CPP_BANCO_EFECTIVO");
        assertThat(creado.getCuentaContable()).isSameAs(hoja);
    }

    @Test
    @DisplayName("crear rechaza una clave en minúsculas que duplica una existente")
    void crear_claveMinusculasDuplicada_rechaza() {
        when(parametroRepo.findByClave("CPP_CUENTA_COMPRAS"))
                .thenReturn(Optional.of(ParametroContable.builder().id(1L).clave("CPP_CUENTA_COMPRAS").build()));

        assertThatThrownBy(() -> service.crear("cpp_cuenta_compras", "Compras", 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un parámetro con la clave: CPP_CUENTA_COMPRAS");
        verify(parametroRepo, never()).save(any());
    }

    @Test
    @DisplayName("actualizar rechaza mover una clave a una cuenta agrupadora")
    void actualizar_cuentaAgrupadora_rechaza() {
        when(parametroRepo.findById(4L))
                .thenReturn(Optional.of(ParametroContable.builder().id(4L).clave("CPP_BANCO_TRANSFERENCIA").build()));
        when(cuentaRepo.findById(2L)).thenReturn(Optional.of(cuenta(2L, "1.1.02", true, true)));
        when(cuentaRepo.countSubcuentas(2L)).thenReturn(1L);

        assertThatThrownBy(() -> service.actualizar(4L, "CPP_BANCO_TRANSFERENCIA", "Banco", 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1.1.02")
                .hasMessageContaining("subcuentas");
        verify(parametroRepo, never()).save(any());
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
}
