package api.astro.whats_orders_manager.modules.facturacion.exception;

/**
 * Los efectos de una nota aceptada no se pueden aplicar (p. ej. la factura tiene pagos aplicados). Se lanza dentro de
 * la transacción que guarda el estado del comprobante: nada se persiste y la nota sigue EN_TRAMITE hasta que se
 * corrija la causa y el sondeo vuelva a consultar (T-032, criterios 27 y 31).
 */
public class NotaFacturaEfectosException extends RuntimeException {

    public NotaFacturaEfectosException(String message) {
        super(message);
    }
}
