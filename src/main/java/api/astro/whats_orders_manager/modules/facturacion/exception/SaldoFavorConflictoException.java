package api.astro.whats_orders_manager.modules.facturacion.exception;

/** El saldo a favor no admite la operación pedida (aplicar, reversar): se responde 409 con este mensaje fijo (T-048). */
public class SaldoFavorConflictoException extends RuntimeException {

    public SaldoFavorConflictoException(String message) {
        super(message);
    }
}
