package api.astro.whats_orders_manager.modules.facturacion.enums;

/** Resultado de guardar un tipo de cambio obtenido de un proveedor externo (T-008). */
public enum EstadoSincronizacionTipoCambio {
    /** No había tasa activa para ese día y moneda: se creó. */
    CREADO,
    /** Había una tasa activa de la misma fuente con otro valor: se actualizó. */
    ACTUALIZADO,
    /** Había una tasa activa de la misma fuente con el mismo valor: no se escribió nada. */
    SIN_CAMBIOS,
    /** Había una tasa activa de otra fuente (p. ej. MANUAL), que tiene prioridad: se conservó. */
    CONSERVADO,
    /** El proveedor falló o devolvió datos inválidos: no se escribió nada. */
    ERROR
}
