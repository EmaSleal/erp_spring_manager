package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

/**
 * Clasificación en origen de un POST /recepcion a Hacienda (ADR-030 §3). Determina el estado del comprobante.
 */
public enum ResultadoEnvioHacienda {

    /** Hacienda confirmó la recepción (2xx: 201 según la documentación, 202 en la práctica) → ENVIADO. */
    RECIBIDO(EstadoComprobante.ENVIADO),

    /** Es seguro que Hacienda no lo tiene (no hubo conexión, token, o 4xx que no indica recepción) → NO_TRANSMITIDO. */
    NO_TRANSMITIDO(EstadoComprobante.NO_TRANSMITIDO),

    /** Hacienda pudo recibirlo (timeout de lectura, conexión cortada, 5xx, 4xx de ya recibido o no clasificable). */
    ENVIO_INCIERTO(EstadoComprobante.ENVIO_INCIERTO),

    /** Falla local al preparar el envío, antes de cualquier conexión (configuración, codificación) → ERROR. */
    ERROR_LOCAL(EstadoComprobante.ERROR);

    private final EstadoComprobante estado;

    ResultadoEnvioHacienda(EstadoComprobante estado) {
        this.estado = estado;
    }

    /** Estado que toma el comprobante tras el intento de envío. */
    public EstadoComprobante estado() {
        return estado;
    }
}
