package api.astro.whats_orders_manager.modules.facturacion.enums;

/** Estado de un saldo a favor del cliente (ADR-053). */
public enum EstadoSaldoFavor {
    /** Tiene saldo vivo y se puede aplicar. */
    VIGENTE,
    /** Su saldo vivo llegó a 0. Una reversión lo devuelve a VIGENTE. */
    AGOTADO
}
