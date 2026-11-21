package api.astro.whats_orders_manager.modules.facturacion.enums;

/** Estado de una aplicación de un saldo a favor a una factura (ADR-057). */
public enum EstadoAplicacionSaldoFavor {
    VIGENTE,
    /** Reversada con razón y asiento de reversión; nada se borra. */
    REVERSADA
}
