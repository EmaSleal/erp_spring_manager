package api.astro.whats_orders_manager.modules.facturacion.enums;

/** Origen del tipo de cambio del día guardado en una aplicación (ADR-056 §3): API (BCCR / proveedor) o MANUAL. */
public enum OrigenTipoCambio {
    API,
    MANUAL;

    /** MANUAL → MANUAL; BCCR y API_EXTERNA → API. */
    public static OrigenTipoCambio de(FuenteTipoCambio fuente) {
        return fuente == FuenteTipoCambio.MANUAL ? MANUAL : API;
    }
}
