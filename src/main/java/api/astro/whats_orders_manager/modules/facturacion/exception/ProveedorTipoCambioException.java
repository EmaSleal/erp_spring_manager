package api.astro.whats_orders_manager.modules.facturacion.exception;

/**
 * El proveedor externo de tipo de cambio no respondió o devolvió datos inválidos
 * (timeout, HTTP distinto de 200, JSON malformado, valor &lt;= 0, moneda no soportada).
 */
public class ProveedorTipoCambioException extends RuntimeException {

    public ProveedorTipoCambioException(String message) {
        super(message);
    }

    public ProveedorTipoCambioException(String message, Throwable cause) {
        super(message, cause);
    }
}
