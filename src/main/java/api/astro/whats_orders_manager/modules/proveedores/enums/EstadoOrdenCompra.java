package api.astro.whats_orders_manager.modules.proveedores.enums;

public enum EstadoOrdenCompra {
    BORRADOR,
    PENDIENTE,
    APROBADA,
    ENVIADA,
    RECIBIDA_PARCIAL,
    RECIBIDA,
    CANCELADA;

    /** ¿Admite registrar facturas del proveedor (CxP)? Solo la OC recibida, total o parcialmente (T-053, ADR-066 §4). */
    public boolean admiteFacturar() {
        return this == RECIBIDA || this == RECIBIDA_PARCIAL;
    }
}
