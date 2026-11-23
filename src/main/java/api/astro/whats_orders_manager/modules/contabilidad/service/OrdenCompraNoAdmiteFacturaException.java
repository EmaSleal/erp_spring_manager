package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;

/** La orden de compra no está en un estado que admita registrar facturas del proveedor (T-053, ADR-066 §4): 409. */
public class OrdenCompraNoAdmiteFacturaException extends IllegalStateException {

    public OrdenCompraNoAdmiteFacturaException(EstadoOrdenCompra estado) {
        super("La orden de compra no admite facturas en estado " + estado
            + "; debe estar recibida total o parcialmente");
    }
}
