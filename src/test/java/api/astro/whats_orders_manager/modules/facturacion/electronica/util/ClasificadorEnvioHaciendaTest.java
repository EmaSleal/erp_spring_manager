package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-025 revisión 2 — el texto documentado de "ya fue recibido" se reconoce de forma explícita y ninguna causa de
 * validación (las que llevan a NO_TRANSMITIDO) coincide con él.
 */
@DisplayName("ClasificadorEnvioHacienda — 'ya fue recibido' frente a causas de validación")
class ClasificadorEnvioHaciendaTest {

    private static final String CLAVE = "50601011600310112345600100010100000000011999999999";

    /** Formas del X-Error-Cause de "ya fue recibido": documentada, sin corchetes, mayúsculas, con tilde. */
    private static final List<String> YA_RECIBIDO = List.of(
            "El comprobante con la Clave [" + CLAVE + "] ya fue recibido.",
            "El comprobante con la Clave " + CLAVE + " ya fue recibido.",
            "EL COMPROBANTE CON LA CLAVE [" + CLAVE + "] YA FUE RECIBIDO.",
            "El comprobante con la clave [" + CLAVE + "] ya fué recibido");

    @Test
    @DisplayName("ninguna causa de validación coincide con el texto de 'ya fue recibido'")
    void ningunPatronDeValidacionCoincideConYaRecibido() {
        assertThat(ClasificadorEnvioHacienda.PATRONES_VALIDACION).isNotEmpty();
        for (String causa : YA_RECIBIDO) {
            String normalizada = ClasificadorEnvioHacienda.normalizar(causa);
            assertThat(ClasificadorEnvioHacienda.PATRONES_VALIDACION)
                    .as(causa)
                    .noneMatch(normalizada::contains);
            assertThat(ClasificadorEnvioHacienda.esYaRecibidoDocumentado(causa)).as(causa).isTrue();
            assertThat(ClasificadorEnvioHacienda.porRespuesta(400, causa)).as(causa)
                    .isEqualTo(ResultadoEnvioHacienda.ENVIO_INCIERTO);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"El XML no cumple con el esquema de validación", "La firma del comprobante no es válida",
            "El campo NumeroConsecutivo es requerido"})
    @DisplayName("una causa de validación no se confunde con 'ya fue recibido' → NO_TRANSMITIDO")
    void validacion_noEsYaRecibido(String causa) {
        assertThat(ClasificadorEnvioHacienda.esYaRecibidoDocumentado(causa)).isFalse();
        assertThat(ClasificadorEnvioHacienda.porRespuesta(400, causa)).isEqualTo(ResultadoEnvioHacienda.NO_TRANSMITIDO);
    }

    @ParameterizedTest(name = "causa «{0}»")
    @ValueSource(strings = {"", "Ocurrió una situación no documentada"})
    @DisplayName("regla prudente: 400 sin causa o con causa no clasificable → ENVIO_INCIERTO")
    void reglaPrudente(String causa) {
        assertThat(ClasificadorEnvioHacienda.porRespuesta(400, causa)).isEqualTo(ResultadoEnvioHacienda.ENVIO_INCIERTO);
    }
}
