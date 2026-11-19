package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

/**
 * La configuración de Hacienda que usa el envío no se pudo obtener (T-041): no hay una activa, hay más de una, es de
 * otra empresa o sus credenciales no se descifran. {@link #getMessage()} lleva el detalle técnico (solo para el log);
 * {@link #getMensajeUsuario()} es un texto fijo, el único que llega a ultimoError y a las respuestas. Es
 * IllegalStateException para conservar el tipo de antes.
 */
public class ConfiguracionHaciendaNoDisponibleException extends IllegalStateException {

    /** Sin configuración activa, más de una, de otra empresa o incompleta. */
    public static final String MENSAJE_NO_VALIDA = "No hay una configuración de Hacienda válida; revise "
        + "Facturación → Configuración de Hacienda (una sola activa, de la empresa, con sus datos completos).";

    /** Las credenciales guardadas no se descifran con la clave del servidor (HACIENDA_CRYPTO_KEY). */
    public static final String MENSAJE_ILEGIBLE = "No se pueden leer las credenciales de Hacienda; revise la "
        + "configuración y la clave de cifrado del servidor.";

    private final String mensajeUsuario;

    public ConfiguracionHaciendaNoDisponibleException(String detalleTecnico, String mensajeUsuario) {
        super(detalleTecnico);
        this.mensajeUsuario = mensajeUsuario;
    }

    public ConfiguracionHaciendaNoDisponibleException(String detalleTecnico, String mensajeUsuario, Throwable causa) {
        super(detalleTecnico, causa);
        this.mensajeUsuario = mensajeUsuario;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }
}
