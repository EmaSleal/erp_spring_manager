package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.AmbienteDistintoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-044 (criterio 1): la regla única de ambiente. */
@DisplayName("T-044 — AmbienteComprobanteGuard")
class AmbienteComprobanteGuardTest {

    private final ConfiguracionHaciendaService configuraciones = mock(ConfiguracionHaciendaService.class);
    private final AmbienteComprobanteGuard guard = new AmbienteComprobanteGuard(configuraciones, null);

    private static ComprobanteElectronico comprobante(AmbienteHacienda ambiente) {
        return ComprobanteElectronico.builder().id(11L).claveNumerica("506X").ambiente(ambiente).build();
    }

    private void activa(AmbienteHacienda ambiente) {
        when(configuraciones.obtenerConfiguracionActiva())
            .thenReturn(ConfiguracionHacienda.builder().id(3L).ambiente(ambiente).build());
    }

    @Test
    @DisplayName("lee el ambiente de la configuración activa")
    void ambienteActivo() {
        activa(AmbienteHacienda.PRODUCCION);
        assertThat(guard.ambienteActivo()).contains(AmbienteHacienda.PRODUCCION);
    }

    @Test
    @DisplayName("mismo ambiente: coincide y no lanza; distinto, en las dos direcciones: no coincide y lanza el mensaje fijo")
    void coincideYExigir() {
        activa(AmbienteHacienda.PRODUCCION);
        assertThat(guard.coincide(comprobante(AmbienteHacienda.PRODUCCION))).isTrue();
        assertThatCode(() -> guard.exigirMismoAmbiente(comprobante(AmbienteHacienda.PRODUCCION))).doesNotThrowAnyException();
        assertThat(guard.coincide(comprobante(AmbienteHacienda.SANDBOX))).isFalse();
        assertThatThrownBy(() -> guard.exigirMismoAmbiente(comprobante(AmbienteHacienda.SANDBOX)))
            .isInstanceOf(AmbienteDistintoException.class)
            .hasMessage("El comprobante pertenece al ambiente SANDBOX y la configuración activa es PRODUCCION; no se envía");

        activa(AmbienteHacienda.SANDBOX);
        assertThatThrownBy(() -> guard.exigirMismoAmbiente(comprobante(AmbienteHacienda.PRODUCCION)))
            .isInstanceOf(AmbienteDistintoException.class)
            .extracting(e -> ((AmbienteDistintoException) e).getMensajeUsuario())
            .isEqualTo("El comprobante pertenece al ambiente PRODUCCION y la configuración activa es SANDBOX; no se envía");
    }

    @Test
    @DisplayName("comprobante sin ambiente (null): no coincide con ninguno, no se envía ni se consulta")
    void ambienteNulo() {
        activa(AmbienteHacienda.SANDBOX);
        assertThat(guard.coincide(comprobante(null))).isFalse();
        assertThatThrownBy(() -> guard.exigirMismoAmbiente(comprobante(null)))
            .isInstanceOf(AmbienteDistintoException.class).hasMessageContaining("SIN_AMBIENTE");
    }

    @Test
    @DisplayName("configuración ilegible o inexistente: la regla no decide (sigue T-041) y no inventa mensajes")
    void sinConfiguracionLegible() {
        when(configuraciones.obtenerConfiguracionActiva()).thenThrow(new ConfiguracionHaciendaNoDisponibleException(
            "detalle", ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA));
        assertThat(guard.ambienteActivo()).isEmpty();
        assertThat(guard.coincide(comprobante(AmbienteHacienda.PRODUCCION))).isTrue();
        assertThatCode(() -> guard.exigirMismoAmbiente(comprobante(AmbienteHacienda.PRODUCCION))).doesNotThrowAnyException();

        org.mockito.Mockito.doThrow(new IllegalStateException("no descifra")).when(configuraciones).obtenerConfiguracionActiva();
        assertThat(guard.ambienteActivo()).isEmpty();
        assertThat(guard.motivoReferencia(comprobante(AmbienteHacienda.PRODUCCION))).isEmpty();
    }

    @Test
    @DisplayName("mensaje de referencia: «La factura se emitió en el ambiente X; la configuración activa es Y»")
    void mensajeReferencia() {
        activa(AmbienteHacienda.PRODUCCION);
        assertThat(guard.motivoReferencia(comprobante(AmbienteHacienda.SANDBOX)))
            .contains("La factura se emitió en el ambiente SANDBOX; la configuración activa es PRODUCCION");
        assertThat(guard.motivoReferencia(comprobante(AmbienteHacienda.PRODUCCION))).isEmpty();
    }

    @Test
    @DisplayName("helpers de listado: motivo solo si hay regla y no coincide")
    void helpersDeListado() {
        assertThat(AmbienteComprobanteGuard.motivo(AmbienteHacienda.SANDBOX, Optional.of(AmbienteHacienda.PRODUCCION)))
            .isEqualTo("El comprobante pertenece al ambiente SANDBOX y la configuración activa es PRODUCCION; no se envía");
        assertThat(AmbienteComprobanteGuard.motivo(AmbienteHacienda.SANDBOX, Optional.of(AmbienteHacienda.SANDBOX))).isNull();
        assertThat(AmbienteComprobanteGuard.motivo(AmbienteHacienda.SANDBOX, Optional.empty())).isNull();
        assertThat(AmbienteComprobanteGuard.coincide(AmbienteHacienda.SANDBOX, Optional.empty())).isTrue();
    }
}
