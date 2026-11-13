package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

/** Consecutivo fuera del rango que admite la clave numérica (T-033). Se responde 400. */
public class ConsecutivoInvalidoException extends IllegalArgumentException {

    public ConsecutivoInvalidoException(String message) {
        super(message);
    }
}
