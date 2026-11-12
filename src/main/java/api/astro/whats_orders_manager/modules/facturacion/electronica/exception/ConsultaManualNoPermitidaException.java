package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

/**
 * "Consultar estado en Hacienda" no aplica al estado del comprobante (ADR-031 §4): solo ENVIADO, PROCESANDO,
 * ENVIO_INCIERTO y ERROR_HACIENDA. Se responde 409.
 */
public class ConsultaManualNoPermitidaException extends RuntimeException {

    public ConsultaManualNoPermitidaException(String message) {
        super(message);
    }
}
