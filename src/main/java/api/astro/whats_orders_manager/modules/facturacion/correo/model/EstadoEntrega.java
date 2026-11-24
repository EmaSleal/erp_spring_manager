package api.astro.whats_orders_manager.modules.facturacion.correo.model;

/** Estado de una entrega de comprobante por correo (ADR-070). */
public enum EstadoEntrega {
    PENDIENTE, ENVIADO, FALLIDO, OMITIDO_SIN_CORREO, OMITIDO_SANDBOX
}
