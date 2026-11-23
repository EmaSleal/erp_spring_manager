package api.astro.whats_orders_manager.modules.proveedores.service;

/** La orden de compra tiene facturas del proveedor (CxP no anuladas) y no puede cancelarse (T-053, revisión 1): 409. */
public class OrdenCompraConFacturasException extends IllegalStateException {

    public static final String MENSAJE = "La orden de compra tiene facturas registradas; anúlelas antes de cancelarla";

    public OrdenCompraConFacturasException() {
        super(MENSAJE);
    }
}
