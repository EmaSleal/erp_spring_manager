package api.astro.whats_orders_manager.modules.facturacion.util;

import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Pattern;

/** IBAN costarricense (T-049, ADR-058 §4): CR + 20 dígitos con dígito de control ISO 13616 (mod 97). */
public final class IbanCostaRica {

    private static final Pattern FORMATO = Pattern.compile("CR[0-9]{20}");
    private static final BigInteger NOVENTA_Y_SIETE = BigInteger.valueOf(97);

    private IbanCostaRica() {
    }

    /** Sin espacios y en mayúsculas; null si es null. */
    public static String normalizar(String iban) {
        return iban == null ? null : iban.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** true si el IBAN (con o sin espacios) es CR + 20 dígitos y su dígito de control mod 97 es 1. */
    public static boolean esValido(String iban) {
        String n = normalizar(iban);
        if (n == null || !FORMATO.matcher(n).matches()) {
            return false;
        }
        // ISO 13616: los cuatro primeros caracteres van al final y las letras valen A=10 … Z=35 (C=12, R=27)
        String reordenado = n.substring(4) + "1227" + n.substring(2, 4);
        return new BigInteger(reordenado).mod(NOVENTA_Y_SIETE).intValue() == 1;
    }
}
