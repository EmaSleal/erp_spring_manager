package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import java.util.Optional;

/**
 * Teléfono del emisor y del receptor en el XML (T-033, rechazo -20 de Hacienda). NumTelefono lleva solo dígitos, sin
 * el código de país, que va en CodigoPais; el Anexo v4.4 exige de 8 a 20 dígitos.
 */
public final class TelefonoFe {

    /** Código de país que acompaña a NumTelefono. */
    public static final String CODIGO_PAIS = "506";

    private static final int MIN_DIGITOS = 8;
    private static final int MAX_DIGITOS = 20;

    private TelefonoFe() {
    }

    /**
     * Número para NumTelefono: sin separadores y sin el prefijo 506 si al quitarlo quedan al menos 8 dígitos.
     *
     * @return vacío si no queda un número de 8 a 20 dígitos: el nodo Telefono, que es opcional, se omite
     */
    public static Optional<String> normalizar(String telefono) {
        if (telefono == null) {
            return Optional.empty();
        }
        String digitos = telefono.replaceAll("[^0-9]", "");
        if (digitos.startsWith(CODIGO_PAIS) && digitos.length() - CODIGO_PAIS.length() >= MIN_DIGITOS) {
            digitos = digitos.substring(CODIGO_PAIS.length());
        }
        return digitos.length() >= MIN_DIGITOS && digitos.length() <= MAX_DIGITOS
            ? Optional.of(digitos) : Optional.empty();
    }
}
