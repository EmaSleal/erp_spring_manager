package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** T-033 (criterios 10 y 11): NumTelefono sin código de país, de 8 a 20 dígitos. */
@DisplayName("T-033 — normalización del teléfono para el XML")
class TelefonoFeTest {

    @ParameterizedTest(name = "«{0}» → {1}")
    @CsvSource({"'+506 8629-6567',86296567", "50686296567,86296567", "8629-6567,86296567", "(506) 2222 3333,22223333",
            "'22223333',22223333", "'50612345',50612345"})
    @DisplayName("sin separadores ni el 506 cuando al quitarlo quedan 8 dígitos")
    void normaliza(String telefono, String esperado) {
        assertThat(TelefonoFe.normalizar(telefono)).contains(esperado);
    }

    @ParameterizedTest(name = "«{0}» → se omite")
    @ValueSource(strings = {"2222-333", "506", "", "   ", "abc", "123456789012345678901"})
    @DisplayName("si no quedan 8 a 20 dígitos el nodo Telefono se omite")
    void omite(String telefono) {
        assertThat(TelefonoFe.normalizar(telefono)).isEmpty();
    }

    @Test
    @DisplayName("null → se omite")
    void nulo() {
        assertThat(TelefonoFe.normalizar(null)).isEmpty();
    }
}
