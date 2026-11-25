package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.panel.RegimenPanel;
import api.astro.whats_orders_manager.modules.regimen.panel.RegimenPanelService;
import api.astro.whats_orders_manager.modules.regimen.tope.EstadoTope;
import api.astro.whats_orders_manager.modules.regimen.tope.NivelTope;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-056 (criterios 5 y 7): el bloque de régimen del panel /facturas/inicio: aviso de verificación y tope del año del reloj. */
@DisplayName("T-056 — bloque de régimen del panel de Facturación")
class RegimenPanelServiceTest {

    private final VerificacionRegimenService verificacion = mock(VerificacionRegimenService.class);
    private final TopeComprasRtsService tope = mock(TopeComprasRtsService.class);
    private final RelojMutable reloj = new RelojMutable(LocalDate.of(2027, 1, 10));
    private final RegimenPanelService servicio = new RegimenPanelService(verificacion, tope, reloj);

    @Test
    @DisplayName("sin aviso de verificación y año sin parámetros: «Tope de 2027 sin configurar» y la advertencia del ERP")
    void sinParametros() {
        when(verificacion.avisoPanel()).thenReturn(Optional.empty());
        when(tope.estado(2027)).thenReturn(EstadoTope.sinConfigurar(2027));
        RegimenPanel p = servicio.obtener();
        assertThat(p.avisoVerificacion()).isNull();
        assertThat(p.tope().configurado()).isFalse();
        assertThat(p.tope().mensaje()).isEqualTo("Tope de 2027 sin configurar");
        assertThat(p.advertenciaTope()).isEqualTo(TopeComprasRtsService.ADVERTENCIA_SOLO_ERP);
    }

    @Test
    @DisplayName("con aviso de DIFIERE y tope en AVISO: ambos llegan al panel")
    void conAvisos() {
        when(verificacion.avisoPanel()).thenReturn(Optional.of("Hacienda difiere"));
        EstadoTope estado = new EstadoTope(2027, true, new BigDecimal("100"), new BigDecimal("85"), new BigDecimal("85.00"),
                NivelTope.AVISO, new BigDecimal("80"), new BigDecimal("100"), "Compras al 85 %");
        when(tope.estado(2027)).thenReturn(estado);
        RegimenPanel p = servicio.obtener();
        assertThat(p.avisoVerificacion()).isEqualTo("Hacienda difiere");
        assertThat(p.tope()).isEqualTo(estado);
    }

    @Test
    @DisplayName("si los servicios fallan el panel no se cae: bloque vacío")
    void unFalloNoRompeElPanel() {
        when(verificacion.avisoPanel()).thenThrow(new IllegalStateException("x"));
        when(tope.estado(2027)).thenThrow(new IllegalStateException("y"));
        RegimenPanel p = servicio.obtener();
        assertThat(p.avisoVerificacion()).isNull();
        assertThat(p.tope()).isNull();
    }
}
