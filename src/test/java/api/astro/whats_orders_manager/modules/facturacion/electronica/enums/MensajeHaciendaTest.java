package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** T-036 (criterio 8): los códigos de Mensaje según el Anexo v4.4 (nota 11): 1 aceptado, 2 parcial, 3 rechazado. */
@DisplayName("T-036 — enum MensajeHacienda según el Anexo")
class MensajeHaciendaTest {

    @Test
    @DisplayName("recorre los tres códigos del Anexo: 1, 2 y 3")
    void recorreLosTresCodigos() {
        assertThat(MensajeHacienda.fromCodigo("1")).isEqualTo(MensajeHacienda.ACEPTADO);
        assertThat(MensajeHacienda.fromCodigo("2")).isEqualTo(MensajeHacienda.ACEPTADO_PARCIAL);
        assertThat(MensajeHacienda.fromCodigo("3")).isEqualTo(MensajeHacienda.RECHAZADO);
        assertThat(MensajeHacienda.ACEPTADO.getCodigo()).isEqualTo("1");
        assertThat(MensajeHacienda.ACEPTADO_PARCIAL.getCodigo()).isEqualTo("2");
        assertThat(MensajeHacienda.RECHAZADO.getCodigo()).isEqualTo("3");
    }

    @Test
    @DisplayName("1, 2 y 3 no son errores técnicos; los 2xx y 3xx técnicos siguen siéndolo")
    void clasificacion() {
        assertThat(MensajeHacienda.ACEPTADO.esExitoso()).isTrue();
        assertThat(MensajeHacienda.ACEPTADO_PARCIAL.esExitoso()).isFalse();
        assertThat(MensajeHacienda.RECHAZADO.esRechazo()).isTrue();
        assertThat(MensajeHacienda.ACEPTADO_PARCIAL.esRechazo()).isFalse();
        for (MensajeHacienda m : new MensajeHacienda[] {MensajeHacienda.ACEPTADO, MensajeHacienda.ACEPTADO_PARCIAL,
                MensajeHacienda.RECHAZADO}) {
            assertThat(m.esErrorTecnico()).as(m.name()).isFalse();
        }
        assertThat(MensajeHacienda.ERROR_AUTENTICACION.esErrorTecnico()).isTrue();
        assertThat(MensajeHacienda.ERROR_SERVICIO.esErrorTecnico()).isTrue();
    }
}
