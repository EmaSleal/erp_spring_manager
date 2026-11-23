package api.astro.whats_orders_manager.modules.contabilidad.service;

/** El proveedor ya tiene una cuenta por pagar no anulada con ese número de comprobante (T-052, ADR-066): 409. */
public class ComprobanteProveedorDuplicadoException extends IllegalStateException {

    public ComprobanteProveedorDuplicadoException(String numeroComprobante) {
        super("Ya existe una cuenta por pagar de este proveedor con el comprobante " + numeroComprobante);
    }
}
