package api.astro.whats_orders_manager.modules.facturacion.exception;

/**
 * La factura no admite la nota pedida en su estado actual (T-032, criterio 7): se responde 409 con este mensaje.
 */
public class NotaFacturaConflictoException extends RuntimeException {

    public NotaFacturaConflictoException(String message) {
        super(message);
    }
}
