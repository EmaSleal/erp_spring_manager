package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;

/**
 * T-044 (P-099): el comprobante pertenece a un ambiente distinto del de la configuración de Hacienda activa, así que no
 * se envía ni se consulta. No es una falla del documento: quien la lanza no cambia su estado, sus intentos ni su
 * ultimoError. {@link #getMensajeUsuario()} es el texto fijo que llega a las respuestas (409); {@link #getMessage()} es
 * el mismo, sin detalle técnico.
 */
public class AmbienteDistintoException extends IllegalStateException {

    private final AmbienteHacienda ambienteComprobante;
    private final AmbienteHacienda ambienteActivo;

    public AmbienteDistintoException(AmbienteHacienda ambienteComprobante, AmbienteHacienda ambienteActivo) {
        super(mensaje(ambienteComprobante, ambienteActivo));
        this.ambienteComprobante = ambienteComprobante;
        this.ambienteActivo = ambienteActivo;
    }

    /** «El comprobante pertenece al ambiente X y la configuración activa es Y; no se envía». */
    public static String mensaje(AmbienteHacienda ambienteComprobante, AmbienteHacienda ambienteActivo) {
        return "El comprobante pertenece al ambiente " + nombre(ambienteComprobante) + " y la configuración activa es "
            + nombre(ambienteActivo) + "; no se envía";
    }

    /** Nombre del enum (SANDBOX o PRODUCCION); SIN_AMBIENTE si el comprobante no lo tiene. */
    public static String nombre(AmbienteHacienda ambiente) {
        return ambiente == null ? "SIN_AMBIENTE" : ambiente.name();
    }

    public String getMensajeUsuario() {
        return getMessage();
    }

    public AmbienteHacienda getAmbienteComprobante() {
        return ambienteComprobante;
    }

    public AmbienteHacienda getAmbienteActivo() {
        return ambienteActivo;
    }
}
