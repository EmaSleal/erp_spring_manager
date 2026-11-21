package api.astro.whats_orders_manager.modules.facturacion.exception;

/** El pedido sobre un saldo a favor es inválido (p. ej. razón fuera de 3 a 180 caracteres): se responde 400 (T-048). */
public class SaldoFavorPedidoInvalidoException extends RuntimeException {

    public SaldoFavorPedidoInvalidoException(String message) {
        super(message);
    }
}
