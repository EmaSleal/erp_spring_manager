package api.astro.whats_orders_manager.modules.facturacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T-049 — IBAN costarricense (CR + 20 dígitos, mod 97)")
class IbanCostaRicaTest {

    /** IBAN válido calculado con ISO 13616: dígitos de control = 98 − (BBAN + «CR00» en números) mod 97. */
    static String ibanValido(String bban18) {
        BigInteger n = new BigInteger(bban18 + "1227" + "00");
        int control = 98 - n.mod(BigInteger.valueOf(97)).intValue();
        return "CR" + String.format("%02d", control) + bban18;
    }

    @Test
    @DisplayName("acepta IBAN calculados con mod 97, con y sin espacios, y minúsculas")
    void valido() {
        for (String bban : new String[]{"015202001026284066", "000000000000000001", "123456789012345678"}) {
            String iban = ibanValido(bban);
            assertThat(IbanCostaRica.esValido(iban)).as(iban).isTrue();
            String conEspacios = iban.substring(0, 4) + " " + iban.substring(4, 8) + " " + iban.substring(8, 12) + " "
                    + iban.substring(12, 16) + " " + iban.substring(16, 20) + " " + iban.substring(20);
            assertThat(IbanCostaRica.esValido(conEspacios)).as(conEspacios).isTrue();
            assertThat(IbanCostaRica.normalizar(conEspacios.toLowerCase())).isEqualTo(iban);
        }
    }

    @Test
    @DisplayName("rechaza un dígito de control equivocado")
    void digitoDeControlInvalido() {
        String iban = ibanValido("015202001026284066");
        int control = Integer.parseInt(iban.substring(2, 4));
        String malo = "CR" + String.format("%02d", (control + 1) % 100) + iban.substring(4);
        assertThat(IbanCostaRica.esValido(malo)).isFalse();
        // un dígito cambiado del BBAN también rompe el control
        String otro = iban.substring(0, 21) + (iban.charAt(21) == '9' ? '0' : (char) (iban.charAt(21) + 1));
        assertThat(IbanCostaRica.esValido(otro)).isFalse();
    }

    @Test
    @DisplayName("rechaza formato: otro país, largo, letras, vacío y null")
    void formatoInvalido() {
        String iban = ibanValido("015202001026284066");
        assertThat(IbanCostaRica.esValido("DE" + iban.substring(2))).isFalse();
        assertThat(IbanCostaRica.esValido(iban.substring(0, 21))).isFalse();
        assertThat(IbanCostaRica.esValido(iban + "0")).isFalse();
        assertThat(IbanCostaRica.esValido(iban.substring(0, 10) + "A" + iban.substring(11))).isFalse();
        assertThat(IbanCostaRica.esValido("")).isFalse();
        assertThat(IbanCostaRica.esValido("   ")).isFalse();
        assertThat(IbanCostaRica.esValido(null)).isFalse();
        assertThat(IbanCostaRica.normalizar(null)).isNull();
    }
}
