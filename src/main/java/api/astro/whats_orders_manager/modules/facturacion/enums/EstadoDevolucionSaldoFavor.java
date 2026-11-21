package api.astro.whats_orders_manager.modules.facturacion.enums;

/** Estado de una devolución en dinero de un saldo a favor (ADR-058 §3). Nada se borra: solo cambia de estado. */
public enum EstadoDevolucionSaldoFavor {
    /** Pedida por el cliente y registrada; el monto está reservado. */
    SOLICITADA,
    /** Aprobada por un usuario con el permiso, distinto de quien la solicitó y de quien emitió la nota. */
    APROBADA,
    /** Pagada desde una cuenta bancaria de la empresa: el saldo bajó y el asiento existe. No se reversa en B2. */
    PAGADA,
    /** Rechazada por el aprobador, con razón; la reserva se liberó. */
    RECHAZADA,
    /** Anulada antes de pagarse, con razón; la reserva se liberó. */
    ANULADA
}
