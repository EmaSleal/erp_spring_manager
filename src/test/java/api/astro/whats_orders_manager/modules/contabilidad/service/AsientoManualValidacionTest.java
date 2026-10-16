package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-019 (P-031, ADR-014, ADR-017) — validations of the manual journal entry.
 *
 * A draft may be unbalanced or have fewer than 2 lines (even none); saving it (crear/actualizar)
 * rejects a missing date and any line whose account is not an active leaf. Posting (contabilizar)
 * checks the date, at least 2 lines, debits = credits, and re-checks that every account is still an
 * active leaf.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-019 — asiento manual: validaciones de borrador y de contabilización")
class AsientoManualValidacionTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private AsientoContableService service;

    private CuentaContable caja;
    private CuentaContable gasto;

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);
        caja = cuenta(1L, "1.1.01.001", "Caja", true, true);
        gasto = cuenta(2L, "5.1.03.001", "Servicios", true, true);
        lenient().when(cuentaRepository.findById(1L)).thenReturn(Optional.of(caja));
        lenient().when(cuentaRepository.findById(2L)).thenReturn(Optional.of(gasto));
        lenient().when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("crear (borrador)")
    class Crear {

        @Test
        @DisplayName("acepta un borrador descuadrado")
        void aceptaDescuadrado() {
            AsientoContable guardado = service.crear(asiento(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "40.00")));

            assertThat(guardado.getEstado()).isEqualTo(EstadoAsiento.BORRADOR);
            assertThat(guardado.estaCuadrado()).isFalse();
            verify(asientoRepository).save(any(AsientoContable.class));
        }

        @Test
        @DisplayName("acepta un borrador con 1 línea y uno sin líneas")
        void aceptaMenosDeDosLineas() {
            assertThat(service.crear(asiento(FECHA, linea(caja, "100.00", "0"))).getDetalles()).hasSize(1);
            assertThat(service.crear(asiento(FECHA)).getDetalles()).isEmpty();
        }

        @Test
        @DisplayName("rechaza una fecha vacía (sin relleno con la fecha de hoy) y no guarda")
        void rechazaSinFecha() {
            AsientoContable sinFecha = asiento(null, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"));

            assertThatThrownBy(() -> service.crear(sinFecha))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha del asiento es obligatoria");
            assertThat(sinFecha.getFecha()).isNull();
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una línea con una cuenta agrupadora, nombrando la cuenta")
        void rechazaAgrupadora() {
            CuentaContable agrupadora = cuenta(3L, "5.1.02", "Gastos de personal", true, true);
            when(cuentaRepository.findById(3L)).thenReturn(Optional.of(agrupadora));
            lenient().when(cuentaRepository.countSubcuentas(3L)).thenReturn(3L);

            assertThatThrownBy(() -> service.crear(asiento(FECHA, linea(caja, "100.00", "0"), linea(agrupadora, "0", "100.00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5.1.02 - Gastos de personal")
                    .hasMessageContaining("agrupadora");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una línea con una cuenta inactiva, nombrando la cuenta")
        void rechazaInactiva() {
            CuentaContable inactiva = cuenta(4L, "5.1.09.001", "Vieja", false, true);
            when(cuentaRepository.findById(4L)).thenReturn(Optional.of(inactiva));

            assertThatThrownBy(() -> service.crear(asiento(FECHA, linea(caja, "100.00", "0"), linea(inactiva, "0", "100.00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5.1.09.001 - Vieja")
                    .hasMessageContaining("inactiva");
            verify(asientoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar (borrador)")
    class Actualizar {

        private AsientoContable existente;

        @BeforeEach
        void borradorExistente() {
            existente = asiento(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"));
            existente.setIdAsiento(10L);
            existente.setEstado(EstadoAsiento.BORRADOR);
            when(asientoRepository.findById(10L)).thenReturn(Optional.of(existente));
        }

        @Test
        @DisplayName("acepta dejar el borrador descuadrado y conserva la descripción de cada línea")
        void aceptaDescuadrado() {
            AsientoContable cambios = asiento(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "70.00"));
            cambios.getDetalles().get(1).setDescripcion("Línea editada");

            AsientoContable guardado = service.actualizar(10L, cambios);

            assertThat(guardado.estaCuadrado()).isFalse();
            assertThat(guardado.getDetalles()).extracting(DetalleAsiento::getDescripcion)
                    .containsExactly("Línea 1.1.01.001", "Línea editada");
        }

        @Test
        @DisplayName("rechaza una fecha vacía sin modificar el borrador")
        void rechazaSinFecha() {
            assertThatThrownBy(() -> service.actualizar(10L, asiento(null, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha del asiento es obligatoria");
            assertThat(existente.getFecha()).isEqualTo(FECHA);
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una cuenta agrupadora sin modificar el borrador")
        void rechazaAgrupadora() {
            CuentaContable agrupadora = cuenta(3L, "5.1.02", "Gastos de personal", true, true);
            when(cuentaRepository.findById(3L)).thenReturn(Optional.of(agrupadora));
            lenient().when(cuentaRepository.countSubcuentas(3L)).thenReturn(2L);

            assertThatThrownBy(() -> service.actualizar(10L, asiento(FECHA, linea(caja, "100.00", "0"), linea(agrupadora, "0", "100.00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5.1.02 - Gastos de personal");
            assertThat(existente.getDetalles()).extracting(d -> d.getCuenta().getCodigo())
                    .containsExactly("1.1.01.001", "5.1.03.001");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una cuenta inactiva")
        void rechazaInactiva() {
            CuentaContable inactiva = cuenta(4L, "5.1.09.001", "Vieja", false, true);
            when(cuentaRepository.findById(4L)).thenReturn(Optional.of(inactiva));

            assertThatThrownBy(() -> service.actualizar(10L, asiento(FECHA, linea(caja, "100.00", "0"), linea(inactiva, "0", "100.00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inactiva");
            verify(asientoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("contabilizar")
    class Contabilizar {

        private AsientoContable borrador(LocalDate fecha, DetalleAsiento... lineas) {
            AsientoContable a = asiento(fecha, lineas);
            a.setIdAsiento(20L);
            a.setEstado(EstadoAsiento.BORRADOR);
            when(asientoRepository.findById(20L)).thenReturn(Optional.of(a));
            return a;
        }

        @Test
        @DisplayName("contabiliza un borrador cuadrado con 2 líneas hoja activas")
        void contabilizaCuadrado() {
            borrador(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"));

            assertThat(service.contabilizar(20L).getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        }

        @Test
        @DisplayName("rechaza un descuadre con el mensaje del cuadre (no con el del estado)")
        void rechazaDescuadre() {
            AsientoContable a = borrador(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "40.00"));

            assertThatThrownBy(() -> service.contabilizar(20L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no está cuadrado")
                    .hasMessageContaining("100.00")
                    .hasMessageContaining("40.00");
            assertThat(a.getEstado()).isEqualTo(EstadoAsiento.BORRADOR);
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza menos de 2 líneas")
        void rechazaMenosDeDosLineas() {
            borrador(FECHA, linea(caja, "100.00", "0"));

            assertThatThrownBy(() -> service.contabilizar(20L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("al menos 2 movimientos");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza la falta de fecha")
        void rechazaSinFecha() {
            borrador(null, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"));

            assertThatThrownBy(() -> service.contabilizar(20L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha del asiento es obligatoria");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una cuenta que dejó de ser hoja después de guardar el borrador")
        void rechazaCuentaQueDejoDeSerHoja() {
            borrador(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"));
            lenient().when(cuentaRepository.countSubcuentas(2L)).thenReturn(1L);

            assertThatThrownBy(() -> service.contabilizar(20L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5.1.03.001 - Servicios")
                    .hasMessageContaining("agrupadora");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una cuenta que se inactivó después de guardar el borrador")
        void rechazaCuentaInactivada() {
            borrador(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"));
            gasto.setActiva(false);

            assertThatThrownBy(() -> service.contabilizar(20L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5.1.03.001 - Servicios")
                    .hasMessageContaining("inactiva");
            verify(asientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza un asiento que no está en borrador")
        void rechazaNoBorrador() {
            AsientoContable a = borrador(FECHA, linea(caja, "100.00", "0"), linea(gasto, "0", "100.00"));
            a.setEstado(EstadoAsiento.CONTABILIZADO);

            assertThatThrownBy(() -> service.contabilizar(20L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede contabilizarse. Estado actual");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static AsientoContable asiento(LocalDate fecha, DetalleAsiento... lineas) {
        AsientoContable a = new AsientoContable();
        a.setFecha(fecha);
        a.setConcepto("Asiento manual de prueba");
        a.setTipo(TipoAsiento.MANUAL);
        a.setDetalles(new ArrayList<>(List.of(lineas)));
        return a;
    }

    private static DetalleAsiento linea(CuentaContable cuenta, String debe, String haber) {
        DetalleAsiento d = new DetalleAsiento();
        d.setCuenta(cuenta);
        d.setDebe(new BigDecimal(debe));
        d.setHaber(new BigDecimal(haber));
        d.setDescripcion("Línea " + cuenta.getCodigo());
        return d;
    }

    private static CuentaContable cuenta(Long id, String codigo, String nombre, boolean activa, boolean aceptaMovimientos) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(TipoCuenta.ACTIVO);
        c.setActiva(activa);
        c.setAceptaMovimientos(aceptaMovimientos);
        return c;
    }
}
