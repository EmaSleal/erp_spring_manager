package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/** T-036 (criterio 8): HaciendaApiServiceImpl traduce ind-estado y los códigos 1/2/3 del Anexo al enum. */
@DisplayName("T-036 — determinarMensaje de HaciendaApiServiceImpl")
class DeterminarMensajeHaciendaTest {

    private MensajeHacienda determinar(String codigo) {
        HaciendaApiServiceImpl service = new HaciendaApiServiceImpl(null, null, null);
        return ReflectionTestUtils.invokeMethod(service, "determinarMensaje", codigo);
    }

    @Test
    @DisplayName("textuales y numéricos")
    void mapeo() {
        assertThat(determinar("aceptado")).isEqualTo(MensajeHacienda.ACEPTADO);
        assertThat(determinar("1")).isEqualTo(MensajeHacienda.ACEPTADO);
        assertThat(determinar("2")).isEqualTo(MensajeHacienda.ACEPTADO_PARCIAL);
        assertThat(determinar("rechazado")).isEqualTo(MensajeHacienda.RECHAZADO);
        assertThat(determinar("3")).isEqualTo(MensajeHacienda.RECHAZADO);
        assertThat(determinar("procesando")).isEqualTo(MensajeHacienda.PROCESANDO);
        assertThat(determinar("")).isEqualTo(MensajeHacienda.PROCESANDO);
        assertThat(determinar("error")).isEqualTo(MensajeHacienda.ERROR);
    }
}
