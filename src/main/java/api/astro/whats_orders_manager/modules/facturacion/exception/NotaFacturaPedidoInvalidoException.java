package api.astro.whats_orders_manager.modules.facturacion.exception;

/**
 * Pedido de nota inválido (tipo, código de referencia o razón): se responde 400 con este mensaje (T-032).
 */
public class NotaFacturaPedidoInvalidoException extends RuntimeException {

    public NotaFacturaPedidoInvalidoException(String message) {
        super(message);
    }
}
