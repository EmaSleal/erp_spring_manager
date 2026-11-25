package api.astro.whats_orders_manager.modules.regimen.tope;

/** Nivel del acumulado de compras del año respecto del tope del RTS. El orden de declaración es el de gravedad. */
public enum NivelTope {
    /** Por debajo del umbral de aviso. */
    BAJO,
    /** Alcanzó el umbral de aviso (80 % por defecto). */
    AVISO,
    /** Alcanzó el umbral de tope (100 % por defecto). */
    TOPE
}
