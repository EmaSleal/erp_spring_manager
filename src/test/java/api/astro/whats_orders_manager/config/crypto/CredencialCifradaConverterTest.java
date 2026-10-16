package api.astro.whats_orders_manager.config.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-021 — AES-256-GCM con IV aleatorio: ida y vuelta, cifrados distintos, texto plano heredado, alteración
 * detectada, clave inválida rechazada y sin clave nunca se escribe en texto plano. Claves generadas en la prueba.
 */
@DisplayName("T-021 — converter de credenciales cifradas")
class CredencialCifradaConverterTest {

    private static String claveAleatoria(int bytes) {
        byte[] key = new byte[bytes];
        new java.security.SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private final String clave = claveAleatoria(32);
    private final CredencialCifradaConverter converter = new CredencialCifradaConverter(clave);

    @Test
    @DisplayName("Ida y vuelta devuelve el valor original con prefijo enc:v1:")
    void idaYVuelta() {
        String guardado = converter.convertToDatabaseColumn("contraseña-ñ-€");

        assertThat(guardado).startsWith("enc:v1:").doesNotContain("contraseña");
        assertThat(converter.convertToEntityAttribute(guardado)).isEqualTo("contraseña-ñ-€");
    }

    @Test
    @DisplayName("Dos cifrados del mismo valor son distintos (IV aleatorio)")
    void cifradosDistintos() {
        assertThat(converter.convertToDatabaseColumn("mismo"))
                .isNotEqualTo(converter.convertToDatabaseColumn("mismo"));
    }

    @Test
    @DisplayName("null queda null; texto plano heredado se lee tal cual")
    void nullYHeredado() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("clave-vieja")).isEqualTo("clave-vieja");
    }

    @Test
    @DisplayName("Cifrado alterado, corrupto o con otra clave → excepción")
    void alterado_lanza() {
        String guardado = converter.convertToDatabaseColumn("secreto");
        byte[] payload = Base64.getDecoder().decode(guardado.substring(CredencialCipher.PREFIX.length()));
        payload[payload.length - 1] ^= 0x01;
        String alterado = CredencialCipher.PREFIX + Base64.getEncoder().encodeToString(payload);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(alterado))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("alterada");
        assertThatThrownBy(() -> converter.convertToEntityAttribute("enc:v1:%%%"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> converter.convertToEntityAttribute("enc:v1:AAAA"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CredencialCifradaConverter(claveAleatoria(32)).convertToEntityAttribute(guardado))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Clave de longitud distinta de 32 bytes o no Base64 → rechazada sin mostrarla")
    void claveInvalida_rechazada() {
        String corta = claveAleatoria(16);
        assertThatThrownBy(() -> new CredencialCifradaConverter(corta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HACIENDA_CRYPTO_KEY").hasMessageContaining("32 bytes")
                .hasMessageNotContaining(corta);
        assertThatThrownBy(() -> new CredencialCifradaConverter(claveAleatoria(33)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CredencialCifradaConverter("***no-base64***"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Base64");
    }

    @Test
    @DisplayName("Sin clave: escribir lanza IllegalStateException; leer cifrado lanza; leer heredado funciona")
    void sinClave() {
        CredencialCifradaConverter sinClave = new CredencialCifradaConverter("");
        String cifrado = converter.convertToDatabaseColumn("secreto");

        assertThatThrownBy(() -> sinClave.convertToDatabaseColumn("secreto"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HACIENDA_CRYPTO_KEY");
        assertThatThrownBy(() -> sinClave.convertToEntityAttribute(cifrado))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HACIENDA_CRYPTO_KEY");
        assertThat(sinClave.convertToEntityAttribute("plano")).isEqualTo("plano");
    }
}
